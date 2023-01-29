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
 *
 */

package com.tencent.cloud.polaris.discovery;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.discovery.refresh.PolarisRefreshConfiguration;
import com.tencent.cloud.polaris.extend.nacos.NacosContextProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

/**
 * Discovery Auto Configuration for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisDiscoveryEnabled
@Import({PolarisDiscoveryClientConfiguration.class,
		PolarisReactiveDiscoveryClientConfiguration.class, PolarisRefreshConfiguration.class})
public class PolarisDiscoveryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisServiceDiscovery polarisServiceDiscovery(
			@Nullable NacosContextProperties nacosContextProperties,
			PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisDiscoveryHandler polarisDiscoveryHandler) {
		return new PolarisServiceDiscovery(
				nacosContextProperties,
				polarisDiscoveryProperties,
				polarisDiscoveryHandler);
	}
}
