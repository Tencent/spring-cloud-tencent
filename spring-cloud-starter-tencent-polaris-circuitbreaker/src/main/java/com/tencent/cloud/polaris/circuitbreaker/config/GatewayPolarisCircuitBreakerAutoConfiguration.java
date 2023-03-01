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

package com.tencent.cloud.polaris.circuitbreaker.config;

import com.tencent.cloud.polaris.circuitbreaker.ReactivePolarisCircuitBreakerFactory;
import com.tencent.cloud.polaris.circuitbreaker.gateway.PolarisCircuitBreakerFilterFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledFilter;
import org.springframework.cloud.gateway.filter.factory.FallbackHeadersGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * GatewayPolarisCircuitBreakerAutoConfiguration.
 *
 * @author seanyu 2023-02-27
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@AutoConfigureAfter({ReactivePolarisCircuitBreakerAutoConfiguration.class })
@ConditionalOnClass({ DispatcherHandler.class, ReactivePolarisCircuitBreakerAutoConfiguration.class,
		ReactiveCircuitBreakerFactory.class, ReactivePolarisCircuitBreakerFactory.class, GatewayAutoConfiguration.class})
public class GatewayPolarisCircuitBreakerAutoConfiguration {

	@Bean
	@ConditionalOnBean(ReactivePolarisCircuitBreakerFactory.class)
	@ConditionalOnEnabledFilter
	public PolarisCircuitBreakerFilterFactory polarisCircuitBreakerFilterFactory(
			ReactivePolarisCircuitBreakerFactory reactiveCircuitBreakerFactory,
			ObjectProvider<DispatcherHandler> dispatcherHandler
	) {
		return new PolarisCircuitBreakerFilterFactory(reactiveCircuitBreakerFactory, dispatcherHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledFilter
	public FallbackHeadersGatewayFilterFactory fallbackHeadersGatewayFilterFactory() {
		return new FallbackHeadersGatewayFilterFactory();
	}

}
