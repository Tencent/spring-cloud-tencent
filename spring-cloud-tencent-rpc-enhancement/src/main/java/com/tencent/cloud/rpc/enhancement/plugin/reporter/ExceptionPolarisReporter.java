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

package com.tencent.cloud.rpc.enhancement.plugin.reporter;


import java.util.Optional;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.cloud.rpc.enhancement.plugin.PolarisEnhancedPluginUtils;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant.ClientPluginOrder.CONSUMER_REPORTER_PLUGIN_ORDER;

/**
 * Polaris reporter when feign call fails.
 *
 * @author Haotian Zhang
 */
public class ExceptionPolarisReporter implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionPolarisReporter.class);

	private final ConsumerAPI consumerAPI;

	private final RpcEnhancementReporterProperties reportProperties;

	public ExceptionPolarisReporter(RpcEnhancementReporterProperties reportProperties,
			ConsumerAPI consumerAPI) {
		this.reportProperties = reportProperties;
		this.consumerAPI = consumerAPI;
	}

	@Override
	public String getName() {
		return ExceptionPolarisReporter.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.EXCEPTION;
	}

	@Override
	public void run(EnhancedPluginContext context) {
		if (!this.reportProperties.isEnabled()) {
			return;
		}

		EnhancedRequestContext request = context.getRequest();
		ServiceInstance callerServiceInstance = Optional.ofNullable(context.getLocalServiceInstance()).orElse(new DefaultServiceInstance());
		ServiceInstance calleeServiceInstance = Optional.ofNullable(context.getTargetServiceInstance()).orElse(new DefaultServiceInstance());

		ServiceCallResult resultRequest = PolarisEnhancedPluginUtils.createServiceCallResult(
				callerServiceInstance.getHost(),
				calleeServiceInstance.getServiceId(),
				calleeServiceInstance.getHost(),
				calleeServiceInstance.getPort(),
				request.getUrl(),
				request.getHttpHeaders(),
				null,
				null,
				context.getDelay(),
				context.getThrowable()
		);

		LOG.debug("Will report ServiceCallResult of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.getHttpMethod().name(), request.getUrl().getPath(), context.getThrowable().getMessage(), context.getDelay());

		consumerAPI.updateServiceCallResult(resultRequest);

	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("ExceptionPolarisReporter runs failed. context=[{}].",
				context, throwable);
	}

	@Override
	public int getOrder() {
		return CONSUMER_REPORTER_PLUGIN_ORDER;
	}

}
