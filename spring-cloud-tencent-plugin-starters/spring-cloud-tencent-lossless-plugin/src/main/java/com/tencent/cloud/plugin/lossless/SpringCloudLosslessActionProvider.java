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

import com.tencent.cloud.common.util.OkHttpUtil;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessActionProvider;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.DefaultBaseInstance;
import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.http.HttpHeaders;

/**
 * LosslessActionProvider for Spring Cloud.
 *
 * @author Shedfree Wu
 */
public class SpringCloudLosslessActionProvider implements LosslessActionProvider {
	private ServiceRegistry<Registration> serviceRegistry;

	private LosslessProperties losslessProperties;

	private Runnable originalRegisterAction;

	private Registration registration;

	public SpringCloudLosslessActionProvider(ServiceRegistry<Registration> serviceRegistry, Registration registration,
												LosslessProperties losslessProperties, Runnable originalRegisterAction) {
		this.serviceRegistry = serviceRegistry;
		this.registration = registration;
		this.losslessProperties = losslessProperties;
		this.originalRegisterAction = originalRegisterAction;
	}

	@Override
	public String getName() {
		return "spring-cloud";
	}

	@Override
	public void doRegister(InstanceProperties instanceProperties) {
		// use lambda to do original register
		originalRegisterAction.run();
	}

	@Override
	public void doDeregister() {
		serviceRegistry.deregister(registration);
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

	public static BaseInstance getBaseInstance(Registration registration) {
		return getBaseInstance(registration, registration.getPort());
	}

	public static BaseInstance getBaseInstance(Registration registration, Integer port) {
		// for common spring cloud registration, not set namespace
		DefaultBaseInstance baseInstance = new DefaultBaseInstance();
		baseInstance.setService(registration.getServiceId());
		// before web start, port in registration not init
		baseInstance.setPort(port);
		baseInstance.setHost(registration.getHost());
		return baseInstance;
	}
}
