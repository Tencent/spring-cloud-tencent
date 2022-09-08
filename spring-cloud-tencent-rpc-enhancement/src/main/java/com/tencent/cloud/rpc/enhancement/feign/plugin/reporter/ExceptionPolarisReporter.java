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

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

/**
 * Polaris reporter when feign call fails.
 *
 * @author Haotian Zhang
 */
public class ExceptionPolarisReporter implements EnhancedFeignPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionPolarisReporter.class);
	private final RpcEnhancementReporterProperties reporterProperties;
	@Autowired(required = false)
	private ConsumerAPI consumerAPI;

	public ExceptionPolarisReporter(RpcEnhancementReporterProperties reporterProperties) {
		this.reporterProperties = reporterProperties;
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

		if (consumerAPI != null) {
			Request request = context.getRequest();
			Response response = context.getResponse();
			Exception exception = context.getException();
			RetStatus retStatus = RetStatus.RetFail;
			if (exception instanceof SocketTimeoutException) {
				retStatus = RetStatus.RetTimeout;
			}
			LOG.debug("Will report result of {}. Request=[{}]. Response=[{}].", retStatus.name(), request, response);
			ServiceCallResult resultRequest = ReporterUtils.createServiceCallResult(request, retStatus);
			consumerAPI.updateServiceCallResult(resultRequest);
			// update result without method for service circuit break.
			resultRequest.setMethod("");
			consumerAPI.updateServiceCallResult(resultRequest);
		}
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
