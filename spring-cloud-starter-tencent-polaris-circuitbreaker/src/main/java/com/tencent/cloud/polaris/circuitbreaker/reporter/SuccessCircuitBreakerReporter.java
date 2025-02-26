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

package com.tencent.cloud.polaris.circuitbreaker.reporter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreaker;
import com.tencent.cloud.polaris.context.CircuitBreakerStatusCodeException;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.cloud.rpc.enhancement.plugin.PolarisEnhancedPluginUtils;
import com.tencent.cloud.rpc.enhancement.plugin.reporter.SuccessPolarisReporter;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpStatus;

import static com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant.ClientPluginOrder.CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER;

/**
 * SuccessCircuitBreakerReporter.
 *
 * @author sean yu
 */
public class SuccessCircuitBreakerReporter implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(SuccessPolarisReporter.class);

	private final CircuitBreakAPI circuitBreakAPI;

	private final RpcEnhancementReporterProperties reportProperties;

	public SuccessCircuitBreakerReporter(RpcEnhancementReporterProperties reportProperties,
			CircuitBreakAPI circuitBreakAPI) {
		this.reportProperties = reportProperties;
		this.circuitBreakAPI = circuitBreakAPI;
	}

	@Override
	public String getName() {
		return SuccessCircuitBreakerReporter.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.POST;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!this.reportProperties.isEnabled()) {
			return;
		}
		EnhancedRequestContext request = context.getRequest();
		EnhancedResponseContext response = context.getResponse();
		ServiceInstance serviceInstance = Optional.ofNullable(context.getTargetServiceInstance())
				.orElse(new DefaultServiceInstance());

		ResourceStat resourceStat = PolarisEnhancedPluginUtils.createInstanceResourceStat(
				serviceInstance.getServiceId(),
				serviceInstance.getHost(),
				serviceInstance.getPort(),
				request.getUrl(),
				response.getHttpStatus(),
				context.getDelay(),
				null
		);

		LOG.debug("Will report CircuitBreaker ResourceStat of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resourceStat.getRetStatus().name(), request.getHttpMethod().name(), request.getUrl()
						.getPath(), response.getHttpStatus(), context.getDelay());

		circuitBreakAPI.report(resourceStat);

		MetadataObjectValue<PolarisCircuitBreaker> circuitBreakerObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(ContextConstant.CircuitBreaker.POLARIS_CIRCUIT_BREAKER);

		MetadataObjectValue<Long> startTimeMilliObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(ContextConstant.CircuitBreaker.CIRCUIT_BREAKER_START_TIME);

		boolean existCircuitBreaker = existMetadataValue(circuitBreakerObject);
		boolean existStartTime = existMetadataValue(startTimeMilliObject);

		if (existCircuitBreaker && existStartTime) {
			PolarisCircuitBreaker polarisCircuitBreaker = circuitBreakerObject.getObjectValue().get();
			Long startTimeMillis = startTimeMilliObject.getObjectValue().get();
			long delay = System.currentTimeMillis() - startTimeMillis;
			InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
			responseContext.setDuration(delay);
			responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
			HttpStatus status = HttpStatus.resolve(response.getHttpStatus());
			if (status != null && (status.is5xxServerError() || status.is4xxClientError())) {
				Throwable throwable = new CircuitBreakerStatusCodeException(status);
				responseContext.setError(throwable);
			}
			if (responseContext.getError() == null) {
				polarisCircuitBreaker.onSuccess(responseContext);
			}
			else {
				polarisCircuitBreaker.onError(responseContext);
			}
		}
	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("SuccessCircuitBreakerReporter runs failed. context=[{}].",
				context, throwable);
	}

	@Override
	public int getOrder() {
		return CIRCUIT_BREAKER_REPORTER_PLUGIN_ORDER;
	}

	private static boolean existMetadataValue(MetadataObjectValue<?> metadataObjectValue) {
		return Optional.ofNullable(metadataObjectValue).map(MetadataObjectValue::getObjectValue).
				map(Optional::isPresent).orElse(false);
	}
}
