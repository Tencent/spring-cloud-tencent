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

package com.tencent.cloud.rpc.enhancement.feign.plugin.reporter;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;

import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;

/**
 * Polaris reporter when feign call fails.
 *
 * @author Haotian Zhang
 */
public class ExceptionPolarisReporter extends AbstractPolarisReporterAdapter implements EnhancedFeignPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionPolarisReporter.class);
	private final RpcEnhancementReporterProperties reporterProperties;

	private final ConsumerAPI consumerAPI;

	private final CircuitBreakAPI circuitBreakAPI;

	public ExceptionPolarisReporter(RpcEnhancementReporterProperties reporterProperties,
			SDKContext context,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		super(reporterProperties, context);
		this.reporterProperties = reporterProperties;
		this.consumerAPI = consumerAPI;
		this.circuitBreakAPI = circuitBreakAPI;
	}

	@Override
	public String getName() {
		return ExceptionPolarisReporter.class.getName();
	}

	@Override
	public EnhancedFeignPluginType getType() {
		return EnhancedFeignPluginType.EXCEPTION;
	}

	@Override
	public void run(EnhancedFeignContext context) {
		if (!reporterProperties.isEnabled()) {
			return;
		}

		Request request = context.getRequest();
		Response response = context.getResponse();
		Exception exception = context.getException();
		long delay = context.getDelay();

		HttpHeaders requestHeaders = new HttpHeaders();
		request.headers().forEach((s, strings) -> requestHeaders.addAll(s, new ArrayList<>(strings)));
		HttpHeaders responseHeaders = new HttpHeaders();
		Integer status = null;
		if (response != null) {
			response.headers().forEach((s, strings) -> responseHeaders.addAll(s, new ArrayList<>(strings)));
			status = response.status();
		}

		ServiceCallResult resultRequest = createServiceCallResult(
				request.requestTemplate().feignTarget().name(),
				null,
				null,
				URI.create(request.url()),
				requestHeaders,
				responseHeaders,
				status,
				delay,
				exception
		);
		LOG.debug("Will report result of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.httpMethod().name(), request.url(), status, delay);
		consumerAPI.updateServiceCallResult(resultRequest);

		ResourceStat resourceStat = createInstanceResourceStat(
				request.requestTemplate().feignTarget().name(),
				null,
				null,
				URI.create(request.url()),
				status,
				delay,
				exception
		);
		circuitBreakAPI.report(resourceStat);

	}

	@Override
	public void handlerThrowable(EnhancedFeignContext context, Throwable throwable) {
		Request request = context.getRequest();
		Response response = context.getResponse();
		LOG.error("ExceptionPolarisReporter runs failed. Request=[{}]. Response=[{}].", request, response, throwable);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}
}
