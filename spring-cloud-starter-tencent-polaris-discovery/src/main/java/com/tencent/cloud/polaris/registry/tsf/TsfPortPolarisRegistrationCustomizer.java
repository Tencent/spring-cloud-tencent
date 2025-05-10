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

package com.tencent.cloud.polaris.registry.tsf;

import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulDiscoveryProperties;
import com.tencent.cloud.polaris.extend.consul.ConsulDiscoveryUtil;
import com.tencent.cloud.polaris.extend.consul.ConsulHeartbeatProperties;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisRegistrationCustomizer;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.context.ApplicationContext;

/**
 * 服务注册时端口相关逻辑.
 *
 * @author Haotian Zhang
 */
public class TsfPortPolarisRegistrationCustomizer implements PolarisRegistrationCustomizer {

	private final AutoServiceRegistrationProperties autoServiceRegistrationProperties;
	private final ApplicationContext context;
	private final ConsulDiscoveryProperties consulDiscoveryProperties;
	private final TsfCoreProperties tsfCoreProperties;
	private final ConsulHeartbeatProperties consulHeartbeatProperties;
	private final SDKContext sdkContext;

	public TsfPortPolarisRegistrationCustomizer(AutoServiceRegistrationProperties autoServiceRegistrationProperties,
			ApplicationContext context, ConsulDiscoveryProperties consulDiscoveryProperties, TsfCoreProperties tsfCoreProperties,
			ConsulHeartbeatProperties consulHeartbeatProperties, SDKContext sdkContext) {
		this.autoServiceRegistrationProperties = autoServiceRegistrationProperties;
		this.context = context;
		this.consulDiscoveryProperties = consulDiscoveryProperties;
		this.tsfCoreProperties = tsfCoreProperties;
		this.consulHeartbeatProperties = consulHeartbeatProperties;
		this.sdkContext = sdkContext;
	}

	@Override
	public void customize(PolarisRegistration registration) {
		if (consulDiscoveryProperties.getPort() != null) {
			registration.setPort(consulDiscoveryProperties.getPort());
		}
		// we know the port and can set the check
		ConsulDiscoveryUtil.setCheck(autoServiceRegistrationProperties, consulDiscoveryProperties, tsfCoreProperties, context,
				consulHeartbeatProperties, registration, sdkContext.getConfig());
	}
}
