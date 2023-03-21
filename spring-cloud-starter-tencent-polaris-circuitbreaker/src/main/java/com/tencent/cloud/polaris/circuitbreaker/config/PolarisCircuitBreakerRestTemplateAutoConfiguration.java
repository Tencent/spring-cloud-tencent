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

import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerRestTemplateBeanPostProcessor;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PolarisCircuitBreakerRestTemplateAutoConfiguration.
 *
 * @author sean yu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisCircuitBreakerEnabled
@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
@AutoConfigureAfter(PolarisCircuitBreakerAutoConfiguration.class)
public class PolarisCircuitBreakerRestTemplateAutoConfiguration {

	@Bean
	public PolarisCircuitBreakerRestTemplateBeanPostProcessor polarisCircuitBreakerRestTemplateBeanPostProcessor(
			ApplicationContext applicationContext) {
		return new PolarisCircuitBreakerRestTemplateBeanPostProcessor(applicationContext);
	}

}
