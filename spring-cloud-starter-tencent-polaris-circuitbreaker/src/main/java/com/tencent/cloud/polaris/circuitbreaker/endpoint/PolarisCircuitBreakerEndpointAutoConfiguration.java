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

package com.tencent.cloud.polaris.circuitbreaker.endpoint;

import com.tencent.cloud.polaris.circuitbreaker.config.ConditionalOnPolarisCircuitBreakerEnabled;
import com.tencent.cloud.polaris.context.ServiceRuleManager;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The AutoConfiguration for Polaris CircuitBreaker's Endpoint.
 *
 * @author wenxuan70
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnPolarisCircuitBreakerEnabled
public class PolarisCircuitBreakerEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(ServiceRuleManager.class)
	@ConditionalOnMissingBean
	@ConditionalOnAvailableEndpoint
	public PolarisCircuitBreakerEndpoint polarisCircuitBreakerEndpoint(ServiceRuleManager serviceRuleManager) {
		return new PolarisCircuitBreakerEndpoint(serviceRuleManager);
	}

}
