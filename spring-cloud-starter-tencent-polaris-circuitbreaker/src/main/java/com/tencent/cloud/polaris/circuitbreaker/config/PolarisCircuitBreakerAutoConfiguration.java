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

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.polaris.circuitbreaker.PolarisCircuitBreakerFactory;
import com.tencent.cloud.polaris.circuitbreaker.common.CircuitBreakerConfigModifier;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerRestTemplateBeanPostProcessor;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Autoconfiguration for PolarisCircuitBreaker.
 *
 * @author lepdou 2022-03-29
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisCircuitBreakerEnabled
@AutoConfigureAfter(RpcEnhancementAutoConfiguration.class)
public class PolarisCircuitBreakerAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<PolarisCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(CircuitBreakAPI.class)
	public CircuitBreakAPI circuitBreakAPI(SDKContext polarisContext) {
		return CircuitBreakAPIFactory.createCircuitBreakAPIByContext(polarisContext);
	}

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
	public CircuitBreakerFactory polarisCircuitBreakerFactory(CircuitBreakAPI circuitBreakAPI) {
		PolarisCircuitBreakerFactory factory = new PolarisCircuitBreakerFactory(circuitBreakAPI);
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}

	@Bean
	@ConditionalOnBean(RpcEnhancementReporterProperties.class)
	@ConditionalOnMissingBean(CircuitBreakerConfigModifier.class)
	public CircuitBreakerConfigModifier circuitBreakerConfigModifier(RpcEnhancementReporterProperties properties) {
		return new CircuitBreakerConfigModifier(properties);
	}

}
