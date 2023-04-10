/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.rpc.enhancement.webclient;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.RequestLabelUtils;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

public class EnhancedWebClientReporter extends AbstractPolarisReporterAdapter implements ExchangeFilterFunction {
	private static final Logger LOG = LoggerFactory.getLogger(EnhancedWebClientReporter.class);
	private final ConsumerAPI consumerAPI;

	private final CircuitBreakAPI circuitBreakAPI;

	public EnhancedWebClientReporter(RpcEnhancementReporterProperties reportProperties,
			SDKContext context,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		super(reportProperties, context);
		this.consumerAPI = consumerAPI;
		this.circuitBreakAPI = circuitBreakAPI;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (!reportProperties.isEnabled()) {
			return next.exchange(request);
		}

		MetadataContextHolder.get().setLoadbalancer(
				HeaderConstant.INTERNAL_CALLEE_SERVICE_ID,
				request.headers().getFirst(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID)
		);
		MetadataContextHolder.get().setLoadbalancer(
				HeaderConstant.INTERNAL_CALL_START_TIME,
				String.valueOf(System.currentTimeMillis())
		);
		return next.exchange(request)
				.doOnSuccess(response -> instrumentResponse(request, response, null))
				.doOnError(t -> instrumentResponse(request, null, t));
	}

	private void instrumentResponse(ClientRequest request, ClientResponse response, Throwable t) {
		Map<String, String> loadBalancerContext = MetadataContextHolder.get().getLoadbalancerMetadata();
		String serviceId = loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID);
		long delay = System.currentTimeMillis() - Long.parseLong(loadBalancerContext.get(HeaderConstant.INTERNAL_CALL_START_TIME));

		HttpHeaders requestHeaders = request.headers();
		HttpHeaders responseHeaders = null;
		Integer status = null;
		if (response != null) {
			responseHeaders = response.headers().asHttpHeaders();
			status = response.rawStatusCode();
		}

		ServiceCallResult resultRequest = createServiceCallResult(
				serviceId,
				null,
				null,
				request.url(),
				requestHeaders,
				responseHeaders,
				status,
				delay,
				t
		);
		LOG.debug("Will report result of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.method().name(), request.url().getPath(), status, delay);
		consumerAPI.updateServiceCallResult(resultRequest);

		ResourceStat resourceStat = createInstanceResourceStat(
				serviceId,
				null,
				null,
				request.url(),
				status,
				delay,
				t
		);
		circuitBreakAPI.report(resourceStat);
	}
}
