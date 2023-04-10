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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * EnhancedPolarisRestTemplateReporter.
 *
 * @author sean yu
 */
public class EnhancedPolarisRestTemplateReporter extends AbstractPolarisReporterAdapter implements ClientHttpRequestInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedPolarisRestTemplateReporter.class);

	private final ConsumerAPI consumerAPI;

	private final CircuitBreakAPI circuitBreakAPI;


	/**
	 * Constructor With {@link RpcEnhancementReporterProperties} .
	 *
	 * @param reportProperties instance of {@link RpcEnhancementReporterProperties}.
	 */
	public EnhancedPolarisRestTemplateReporter(RpcEnhancementReporterProperties reportProperties,
			SDKContext context,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		super(reportProperties, context);
		this.consumerAPI = consumerAPI;
		this.circuitBreakAPI = circuitBreakAPI;
	}


	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		if (!reportProperties.isEnabled()) {
			return execution.execute(request, body);
		}

		long startTime = System.currentTimeMillis();
		ClientHttpResponse response = null;
		IOException ex = null;
		try {
			response = execution.execute(request, body);
		}
		catch (SocketTimeoutException e) {
			ex = e;
		}
		HttpHeaders requestHeaders = request.getHeaders();
		HttpHeaders responseHeaders = null;
		Integer status = null;
		if (response != null) {
			responseHeaders = response.getHeaders();
			status = response.getRawStatusCode();
		}

		Map<String, String> loadBalancerContext = MetadataContextHolder.get().getLoadbalancerMetadata();
		String targetHost = loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST);
		Integer targetPort = Integer.valueOf(loadBalancerContext.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT));
		long delay = System.currentTimeMillis() - startTime;

		ServiceCallResult resultRequest = createServiceCallResult(
				request.getURI().getHost(),
				targetHost,
				targetPort,
				request.getURI(),
				requestHeaders,
				responseHeaders,
				status,
				delay,
				ex
		);
		LOG.debug("Will report result of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.getMethod(), request.getURI().getPath(), status, delay);
		consumerAPI.updateServiceCallResult(resultRequest);

		ResourceStat resourceStat = createInstanceResourceStat(
				request.getURI().getHost(),
				targetHost,
				targetPort,
				request.getURI(),
				status,
				delay,
				ex
		);
		circuitBreakAPI.report(resourceStat);

		if (ex != null) {
			throw ex;
		}
		return response;
	}

}
