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

package com.tencent.cloud.polaris.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.util.StringUtils;

/**
 * Auto service registration of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisAutoServiceRegistration extends AbstractAutoServiceRegistration<Registration> {

	private static final Logger log = LoggerFactory.getLogger(PolarisAutoServiceRegistration.class);

	private final PolarisRegistration registration;

	public PolarisAutoServiceRegistration(ServiceRegistry<Registration> serviceRegistry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			PolarisRegistration registration) {
		super(serviceRegistry, autoServiceRegistrationProperties);
		this.registration = registration;
	}

	@Override
	protected PolarisRegistration getRegistration() {
		if (this.registration.getPort() <= 0) {
			this.registration.setPort(this.getPort().get());
		}
		return this.registration;
	}

	@Override
	protected PolarisRegistration getManagementRegistration() {
		return null;
	}

	@Override
	protected void register() {
		if (!this.registration.isRegisterEnabled()) {
			log.debug("Registration disabled.");
			return;
		}
		if (this.registration.getPort() <= 0) {
			this.registration.setPort(getPort().get());
		}
		super.register();
	}

	@Override
	protected void registerManagement() {
		if (!this.registration.isRegisterEnabled()) {
			return;
		}
		super.registerManagement();

	}

	@Override
	protected Object getConfiguration() {
		return this.registration.getPolarisProperties();
	}

	@Override
	protected boolean isEnabled() {
		return this.registration.isRegisterEnabled();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected String getAppName() {
		String appName = registration.getPolarisProperties().getService();
		return StringUtils.isEmpty(appName) ? super.getAppName() : appName;
	}
}
