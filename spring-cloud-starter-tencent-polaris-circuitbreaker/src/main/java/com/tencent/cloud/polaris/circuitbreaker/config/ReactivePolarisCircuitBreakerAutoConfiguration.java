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

import com.tencent.cloud.polaris.circuitbreaker.ReactivePolarisCircuitBreakerFactory;
import com.tencent.cloud.polaris.circuitbreaker.common.CircuitBreakerConfigModifier;
import com.tencent.cloud.polaris.circuitbreaker.reporter.ExceptionCircuitBreakerReporter;
import com.tencent.cloud.polaris.circuitbreaker.reporter.SuccessCircuitBreakerReporter;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementAutoConfiguration;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AutoConfiguration for ReactivePolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {"reactor.core.publisher.Mono", "reactor.core.publisher.Flux"})
@ConditionalOnPolarisCircuitBreakerEnabled
@AutoConfigureAfter(RpcEnhancementAutoConfiguration.class)
public class ReactivePolarisCircuitBreakerAutoConfiguration {

	@Autowired(required = false)
	private List<Customizer<ReactivePolarisCircuitBreakerFactory>> customizers = new ArrayList<>();

	@Bean
	@ConditionalOnMissingBean(SuccessCircuitBreakerReporter.class)
	public SuccessCircuitBreakerReporter successCircuitBreakerReporter(RpcEnhancementReporterProperties properties,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new SuccessCircuitBreakerReporter(properties, polarisSDKContextManager.getCircuitBreakAPI());
	}

	@Bean
	@ConditionalOnMissingBean(ExceptionCircuitBreakerReporter.class)
	public ExceptionCircuitBreakerReporter exceptionCircuitBreakerReporter(RpcEnhancementReporterProperties properties,
			PolarisSDKContextManager polarisSDKContextManager) {
		return new ExceptionCircuitBreakerReporter(properties, polarisSDKContextManager.getCircuitBreakAPI());
	}

	@Bean
	@ConditionalOnMissingBean(ReactiveCircuitBreakerFactory.class)
	public ReactiveCircuitBreakerFactory polarisReactiveCircuitBreakerFactory(PolarisSDKContextManager polarisSDKContextManager) {
		ReactivePolarisCircuitBreakerFactory factory = new ReactivePolarisCircuitBreakerFactory(
				polarisSDKContextManager.getCircuitBreakAPI(), polarisSDKContextManager.getConsumerAPI());
		customizers.forEach(customizer -> customizer.customize(factory));
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerConfigModifier.class)
	public CircuitBreakerConfigModifier reactiveCircuitBreakerConfigModifier(RpcEnhancementReporterProperties properties) {
		return new CircuitBreakerConfigModifier(properties);
	}

}
