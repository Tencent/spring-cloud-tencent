/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.rpc.enhancement.instrument.feign;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.RequestTemplate;
import feign.Response;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static feign.Util.checkNotNull;

/**
 * Wrap for {@link Client}.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignClient implements Client {

	private final Client delegate;

	private final EnhancedPluginRunner pluginRunner;

	public EnhancedFeignClient(Client target, EnhancedPluginRunner pluginRunner) {
		this.delegate = checkNotNull(target, "target");
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Response execute(Request request, Options options) throws IOException {
		EnhancedPluginContext enhancedPluginContext = new EnhancedPluginContext();

		HttpHeaders requestHeaders = new HttpHeaders();
		request.headers().forEach((s, strings) -> requestHeaders.addAll(s, new ArrayList<>(strings)));
		URI url = URI.create(request.url());

		URI serviceUrl = url.resolve(request.requestTemplate().url());

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpHeaders(requestHeaders)
				.httpMethod(HttpMethod.valueOf(request.httpMethod().name()))
				.url(url)
				.serviceUrl(serviceUrl)
				.build();
		enhancedPluginContext.setRequest(enhancedRequestContext);
		enhancedPluginContext.setOriginRequest(request);

		enhancedPluginContext.setLocalServiceInstance(pluginRunner.getLocalServiceInstance());
		String svcName = serviceUrl.getHost();
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance(
				String.format("%s-%s-%d", svcName, url.getHost(), url.getPort()),
				svcName, url.getHost(), url.getPort(), url.getScheme().equals("https"));
		// -1 means access directly by url
		if (serviceInstance.getPort() == -1) {
			enhancedPluginContext.setTargetServiceInstance(null, url);
		}
		else {
			enhancedPluginContext.setTargetServiceInstance(serviceInstance, url);
		}

		long startMillis = System.currentTimeMillis();
		try {
			// Run pre enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.PRE, enhancedPluginContext);

			Response response = delegate.execute(request, options);
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);

			HttpHeaders responseHeaders = new HttpHeaders();
			response.headers().forEach((s, strings) -> responseHeaders.addAll(s, new ArrayList<>(strings)));

			EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
					.httpStatus(response.status())
					.httpHeaders(responseHeaders)
					.build();
			enhancedPluginContext.setResponse(enhancedResponseContext);

			// Run post enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.POST, enhancedPluginContext);
			return response;
		}
		catch (CallAbortedException callAbortedException) {
			// circuit breaker fallback, not need to run post/exception enhanced plugins.
			if (callAbortedException.getFallbackInfo() != null) {
				return getFallbackResponse(callAbortedException.getFallbackInfo());
			}
			else {
				throw callAbortedException;
			}
		}
		catch (IOException origin) {
			enhancedPluginContext.setDelay(System.currentTimeMillis() - startMillis);
			enhancedPluginContext.setThrowable(origin);
			// Run exception enhanced feign plugins.
			pluginRunner.run(EnhancedPluginType.Client.EXCEPTION, enhancedPluginContext);
			throw origin;
		}
		finally {
			// Run finally enhanced plugins.
			pluginRunner.run(EnhancedPluginType.Client.FINALLY, enhancedPluginContext);
		}
	}

	private Response getFallbackResponse(CircuitBreakerStatus.FallbackInfo fallbackInfo) {

		Response.Builder responseBuilder = Response.builder()
				.status(fallbackInfo.getCode());
		if (fallbackInfo.getHeaders() != null) {
			Map<String, Collection<String>> headers = new HashMap<>();
			fallbackInfo.getHeaders().forEach((k, v) -> headers.put(k, Collections.singleton(v)));
			responseBuilder.headers(headers);
		}
		if (fallbackInfo.getBody() != null) {
			responseBuilder.body(fallbackInfo.getBody(), StandardCharsets.UTF_8);
		}
		// Feign Response need a nonnull Request,
		// which is not important in fallback response (no real request),
		// so we create a fake one
		Request fakeRequest = Request.create(Request.HttpMethod.GET, "/", new HashMap<>(), Request.Body.empty(), new RequestTemplate());
		responseBuilder.request(fakeRequest);

		return responseBuilder.build();
	}
}
