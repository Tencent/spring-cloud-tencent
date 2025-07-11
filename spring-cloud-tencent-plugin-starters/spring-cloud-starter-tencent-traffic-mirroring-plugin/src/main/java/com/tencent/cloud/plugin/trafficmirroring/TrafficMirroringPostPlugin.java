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

package com.tencent.cloud.plugin.trafficmirroring;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.plugin.trafficmirroring.config.TrafficMirroringProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.utils.EnhancedRequestUtils;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

import static com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant.ClientPluginOrder.TRAFFIC_MIRRORING_PLUGIN_ORDER;

/**
 * Traffic mirroring post plugin.
 *
 * @author Haotian Zhang
 */
public class TrafficMirroringPostPlugin implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(TrafficMirroringPostPlugin.class);

	private final TrafficMirroringProperties trafficMirroringProperties;

	private final ExecutorService executorService;

	private final HttpClient httpClient;

	public TrafficMirroringPostPlugin(TrafficMirroringProperties trafficMirroringProperties) {
		this.trafficMirroringProperties = trafficMirroringProperties;
		this.executorService = Executors.newFixedThreadPool(trafficMirroringProperties.getRequestPoolSize());
		// create JDK HttpClient instance.
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(trafficMirroringProperties.getRequestConnectionTimeout()))
				.executor(executorService)
				.build();
	}

	@Override
	public String getName() {
		return TrafficMirroringPostPlugin.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.POST;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!trafficMirroringProperties.isEnabled()) {
			return;
		}

		Object originalRequest = context.getOriginRequest();
		// only RestTemplate and Feign request can be mirrored.
		if (!(ClassUtils.isClassPresent("org.springframework.http.HttpRequest")
				&& originalRequest instanceof HttpRequest)
				&& !(ClassUtils.isClassPresent("feign.Request")
				&& originalRequest instanceof Request)) {
			return;
		}

		// get traffic mirroring address.
		String destination = null;
		try {
			MetadataObjectValue<Node> metadataValue = MetadataContextHolder.get()
					.getMetadataContainer(MetadataType.CUSTOM, false)
					.getMetadataValue(RouterConstants.TRAFFIC_MIRRORING_NODE_KEY);
			if (metadataValue != null) {
				Optional<Node> trafficMirroringNode = metadataValue.getObjectValue();
				if (trafficMirroringNode.isPresent()) {
					destination = trafficMirroringNode.get().getHostPort();
				}
			}
		}
		catch (Throwable throwable) {
			LOG.warn("Get traffic mirroring node failed.", throwable);
		}
		if (StringUtils.isBlank(destination)) {
			return;
		}

		byte[] body = context.getOriginBody();
		// async send mirroring request.
		String finalDestination = destination;
		executorService.submit(() -> {
			try {
				sendMirroringRequest(originalRequest, body, finalDestination);
			}
			catch (Exception e) {
				LOG.warn("Traffic mirroring request failed.", e);
			}
		});
	}

	private void setHeaders(java.net.http.HttpRequest.Builder requestBuilder, Object originalRequest) {
		if (ClassUtils.isClassPresent("org.springframework.http.HttpRequest")
				&& originalRequest instanceof HttpRequest) {
			HttpHeaders httpHeaders = ((HttpRequest) originalRequest).getHeaders();
			httpHeaders.forEach((headerName, headerValues) -> {
				if (shouldKeepHeader(headerName)) {
					headerValues.forEach(headerValue ->
							requestBuilder.header(headerName, headerValue));
				}
			});
		}
		else if (ClassUtils.isClassPresent("feign.Request")
				&& originalRequest instanceof Request) {
			Request request = (Request) originalRequest;
			request.headers().forEach((headerName, headerValues) -> {
				if (shouldKeepHeader(headerName)) {
					headerValues.forEach(headerValue ->
							requestBuilder.header(headerName, headerValue));
				}
			});
		}
	}

	/**
	 * Send mirroring request to specified destination.
	 */
	private void sendMirroringRequest(Object originalRequest, byte[] originalBody, String destination) {
		// rebuilt mirroring url: replace original host with specified destination.
		URI originalUri = EnhancedRequestUtils.getUri(originalRequest);
		String method = EnhancedRequestUtils.getMethod(originalRequest);
		String mirroringUrl = rebuildUrlWithDestination(originalUri, destination);
		if (StringUtils.isBlank(mirroringUrl)) {
			LOG.debug("Traffic mirroring request url is empty.");
			return;
		}
		if (originalBody == null) {
			originalBody = new byte[0];
		}

		// create JDK HttpRequest builder.
		java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
				.uri(URI.create(mirroringUrl))
				.method(method,
						java.net.http.HttpRequest.BodyPublishers.ofByteArray(originalBody));

		// copy request headers (except some special headers).
		setHeaders(requestBuilder, originalRequest);

		// build request.
		java.net.http.HttpRequest mirrorRequest = requestBuilder.build();

		// send async request (ignore response).
		httpClient.sendAsync(mirrorRequest, HttpResponse.BodyHandlers.discarding())
				.thenAccept(response -> {
					if (response.statusCode() >= 400) {
						LOG.warn("Traffic mirroring async request return: {}", response.statusCode());
					}
					else {
						LOG.debug("Traffic mirroring async request return: {}", response.statusCode());
					}
				})
				.exceptionally(ex -> {
					LOG.warn("Traffic mirroring async request failed", ex);
					return null;
				});
	}

	/**
	 * Rebuild url with specified destination.
	 */
	private String rebuildUrlWithDestination(URI originalUri, String destination) {
		if (StringUtils.isBlank(destination)) {
			return null;
		}

		String scheme = originalUri.getScheme();
		String path = originalUri.getRawPath();
		String query = originalUri.getRawQuery();

		// fill default port.
		if (!StringUtils.contains(destination, ":")) {
			destination += (scheme.equals("https") ? ":443" : ":80");
		}

		// build full mirroring url.
		String finalUrl = String.format("%s://%s%s%s",
				scheme,
				destination,
				path != null ? path : "",
				query != null ? "?" + query : "");
		LOG.debug("Traffic mirroring request url: {}", finalUrl);

		return finalUrl;
	}

	/**
	 * check header should be kept.
	 */
	private boolean shouldKeepHeader(String headerName) {
		return !headerName.equalsIgnoreCase("connection") &&
				!headerName.equalsIgnoreCase("content-length") &&
				!headerName.equalsIgnoreCase("expect") &&
				!headerName.equalsIgnoreCase("host") &&
				!headerName.equalsIgnoreCase("upgrade");
	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("TrafficMirroringPostPlugin runs failed. context=[{}].", context, throwable);
	}

	@Override
	public int getOrder() {
		return TRAFFIC_MIRRORING_PLUGIN_ORDER;
	}
}
