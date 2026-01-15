/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.router.RouterConfigModifier;
import com.tencent.cloud.polaris.router.config.properties.PolarisMetadataRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNamespaceRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RouterConfigModifierAutoConfiguration.
 *
 * @author sean yu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@Import({PolarisNearByRouterProperties.class, PolarisMetadataRouterProperties.class, PolarisRuleBasedRouterProperties.class,
		PolarisNamespaceRouterProperties.class})
public class RouterConfigModifierAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RouterConfigModifier routerConfigModifier(PolarisNearByRouterProperties polarisNearByRouterProperties) {
		return new RouterConfigModifier(polarisNearByRouterProperties);
	}

}
