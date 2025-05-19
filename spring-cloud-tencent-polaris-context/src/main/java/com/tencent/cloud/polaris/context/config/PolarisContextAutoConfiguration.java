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

package com.tencent.cloud.polaris.context.config;

import java.util.List;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.ModifyAddress;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.context.admin.PolarisAdminConfigModifier;
import com.tencent.cloud.polaris.context.admin.PolarisAdminProperties;
import com.tencent.cloud.polaris.context.config.extend.consul.ConsulProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfInstanceMetadataProvider;
import com.tencent.cloud.polaris.context.event.PushGatewayEventReporterConfigModifier;
import com.tencent.cloud.polaris.context.event.PushGatewayEventReporterProperties;
import com.tencent.cloud.polaris.context.listener.PolarisContextApplicationEventListener;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Autoconfiguration for Polaris {@link SDKContext}.
 *
 * @author Haotian Zhang
 */
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties({PolarisContextProperties.class})
public class PolarisContextAutoConfiguration {

	@Bean(initMethod = "init")
	@ConditionalOnMissingBean
	public PolarisSDKContextManager polarisSDKContextManager(PolarisContextProperties properties, Environment environment, List<PolarisConfigModifier> modifierList) throws PolarisException {
		return new PolarisSDKContextManager(properties, environment, modifierList);
	}

	@Bean
	@ConditionalOnMissingBean
	public ModifyAddress polarisConfigModifier(PolarisContextProperties properties) {
		return new ModifyAddress(properties);
	}

	@Bean
	public ServiceRuleManager serviceRuleManager(PolarisSDKContextManager polarisSDKContextManager) {
		return new ServiceRuleManager(polarisSDKContextManager.getSDKContext(), polarisSDKContextManager.getConsumerAPI());
	}

	@Bean
	public PolarisContextApplicationEventListener contextApplicationEventListener(PolarisSDKContextManager polarisSDKContextManager) {
		return new PolarisContextApplicationEventListener(polarisSDKContextManager);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisAdminProperties polarisAdminProperties() {
		return new PolarisAdminProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisAdminConfigModifier polarisAdminConfigModifier(PolarisAdminProperties polarisAdminProperties) {
		return new PolarisAdminConfigModifier(polarisAdminProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public PushGatewayEventReporterProperties pushGatewayEventReporterProperties() {
		return new PushGatewayEventReporterProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public PushGatewayEventReporterConfigModifier pushGatewayEventReporterConfigModifier(PushGatewayEventReporterProperties pushGatewayEventReporterProperties) {
		return new PushGatewayEventReporterConfigModifier(pushGatewayEventReporterProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ConsulProperties consulProperties() {
		return new ConsulProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public TsfCoreProperties tsfCoreProperties() {
		return new TsfCoreProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public TsfInstanceMetadataProvider tsfInstanceMetadataProvider(TsfCoreProperties tsfCoreProperties) {
		return new TsfInstanceMetadataProvider(tsfCoreProperties);
	}
}
