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

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.util.StringUtils;

/**
 * Auto service registration of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng, changjin wei(魏昌进)
 */
public class PolarisAutoServiceRegistration extends AbstractAutoServiceRegistration<PolarisRegistration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisAutoServiceRegistration.class);

	private final PolarisRegistration registration;

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final AssemblyAPI assemblyAPI;

	public PolarisAutoServiceRegistration(
			ServiceRegistry<PolarisRegistration> serviceRegistry,
			AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			PolarisRegistration registration,
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			AssemblyAPI assemblyAPI
	) {
		super(serviceRegistry, autoServiceRegistrationProperties);
		this.registration = registration;
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.assemblyAPI = assemblyAPI;
	}

	@Override
	protected PolarisRegistration getRegistration() {
		return this.registration;
	}

	@Override
	protected PolarisRegistration getManagementRegistration() {
		return null;
	}

	@Override
	protected void register() {
		if (!this.registration.isRegisterEnabled()) {
			LOGGER.debug("Registration disabled.");
			return;
		}
		if (assemblyAPI != null) {
			assemblyAPI.initService(new ServiceKey(MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE));
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
	protected void deregister() {
		if (!this.registration.isRegisterEnabled()) {
			return;
		}
		super.deregister();
	}

	@Override
	protected void deregisterManagement() {
		if (!this.registration.isRegisterEnabled()) {
			return;
		}
		super.deregisterManagement();
	}

	@Override
	protected Object getConfiguration() {
		return this.polarisDiscoveryProperties;
	}

	@Override
	protected boolean isEnabled() {
		return this.registration.isRegisterEnabled();
	}

	@Override
	@SuppressWarnings("deprecation")
	protected String getAppName() {
		String appName = registration.getServiceId();
		return StringUtils.isEmpty(appName) ? super.getAppName() : appName;
	}
}
