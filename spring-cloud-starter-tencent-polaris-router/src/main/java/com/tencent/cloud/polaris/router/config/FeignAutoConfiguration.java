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

package com.tencent.cloud.polaris.router.config;

import java.util.List;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.feign.PolarisCachingSpringLoadBalanceFactory;
import com.tencent.cloud.polaris.router.feign.RouterLabelFeignInterceptor;
import com.tencent.cloud.polaris.router.spi.FeignRouterLabelResolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * configuration for feign singleton components.
 * Feign-related components need to be loaded only in the feign environment.
 *
 * @author lepdou 2022-06-10
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {"org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer"})
@RibbonClients(defaultConfiguration = {FeignLoadBalancerConfiguration.class})
public class FeignAutoConfiguration {

	@Bean
	public RouterLabelFeignInterceptor routerLabelInterceptor(@Nullable List<FeignRouterLabelResolver> routerLabelResolvers,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		return new RouterLabelFeignInterceptor(routerLabelResolvers, staticMetadataManager, routerRuleLabelResolver);
	}

	@Bean
	public PolarisCachingSpringLoadBalanceFactory polarisCachingSpringLoadBalanceFactory(SpringClientFactory factory) {
		return new PolarisCachingSpringLoadBalanceFactory(factory);
	}
}
