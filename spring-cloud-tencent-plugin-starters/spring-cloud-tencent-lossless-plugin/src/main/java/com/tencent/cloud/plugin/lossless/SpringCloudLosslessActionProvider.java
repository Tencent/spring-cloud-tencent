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

package com.tencent.cloud.plugin.lossless;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.polaris.util.OkHttpUtil;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.http.HttpHeaders;

/**
 * LosslessActionProvider for Spring Cloud.
 *
 * @author Shedfree Wu
 */
public class SpringCloudLosslessActionProvider implements LosslessActionProvider {

	private LosslessProxyServiceRegistry losslessProxyServiceRegistry;

	private LosslessProperties losslessProperties;

	private Consumer<Registration> registrationConsumer;

	private Registration registration;

	public SpringCloudLosslessActionProvider(
			LosslessProxyServiceRegistry losslessProxyServiceRegistry,
			LosslessProperties losslessProperties) {
		this.losslessProxyServiceRegistry = losslessProxyServiceRegistry;
		this.losslessProperties = losslessProperties;
		this.registrationConsumer = losslessProxyServiceRegistry.getTarget()::register;
		this.registration = losslessProxyServiceRegistry.getRegistration();
	}

	@Override
	public String getName() {
		return "spring-cloud";
	}

	@Override
	public void doRegister(InstanceProperties instanceProperties) {
		registrationConsumer.accept(registration);
	}

	@Override
	public void doDeregister() {
		losslessProxyServiceRegistry.deregister();
	}

	/**
	 * Check whether health check is enable.
	 * @return true: register after passing doHealthCheck, false: register after delayRegisterInterval.
	 */
	@Override
	public boolean isEnableHealthCheck() {
		return StringUtils.isNotBlank(losslessProperties.getHealthCheckPath());
	}

	@Override
	public boolean doHealthCheck() {
		Map<String, String> headers = new HashMap<>(1);
		headers.put(HttpHeaders.USER_AGENT, "polaris");

		return OkHttpUtil.checkUrl("localhost", registration.getPort(),
				losslessProperties.getHealthCheckPath(), headers);
	}
}
