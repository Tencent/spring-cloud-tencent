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

package org.springframework.cloud.openfeign;

import com.tencent.cloud.polaris.circuitbreaker.config.ConditionalOnPolarisCircuitBreakerEnabled;
import com.tencent.cloud.polaris.circuitbreaker.feign.PolarisCircuitBreakerNameResolver;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Auto configuration for PolarisFeignCircuitBreakerTargeter.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({Targeter.class})
@ConditionalOnProperty(value = "feign.circuitbreaker.enabled", havingValue = "true")
@AutoConfigureBefore(FeignAutoConfiguration.class)
@ConditionalOnPolarisCircuitBreakerEnabled
public class PolarisFeignCircuitBreakerTargeterAutoConfiguration {

	@Bean
	@Primary
	@ConditionalOnBean(CircuitBreakerFactory.class)
	public Targeter polarisFeignCircuitBreakerTargeter(CircuitBreakerFactory circuitBreakerFactory, PolarisCircuitBreakerNameResolver circuitBreakerNameResolver) {
		return new PolarisFeignCircuitBreakerTargeter(circuitBreakerFactory, circuitBreakerNameResolver);
	}
}
