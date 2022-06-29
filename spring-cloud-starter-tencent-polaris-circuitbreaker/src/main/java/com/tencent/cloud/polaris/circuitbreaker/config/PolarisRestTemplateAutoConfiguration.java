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

import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisResponseErrorHandler;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisRestTemplateModifier;
import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisRestTemplateResponseErrorHandler;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.core.ConsumerAPI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author : wh
 * @date : 2022/6/21 21:34
 * @description: Auto configuration PolarisRestTemplateAutoConfiguration
 */
@ConditionalOnProperty(value = "spring.cloud.polaris.circuitbreaker.enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
public class PolarisRestTemplateAutoConfiguration {

	@Bean
	@ConditionalOnBean(RestTemplate.class)
	public PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler(ConsumerAPI consumerAPI, @Autowired(required = false) PolarisResponseErrorHandler polarisResponseErrorHandler) {
		return new PolarisRestTemplateResponseErrorHandler(consumerAPI, polarisResponseErrorHandler);
	}

	@Bean
	@ConditionalOnBean(RestTemplate.class)
	public PolarisRestTemplateModifier polarisRestTemplateBeanPostProcessor(PolarisRestTemplateResponseErrorHandler restTemplateResponseErrorHandler) {
		return new PolarisRestTemplateModifier(restTemplateResponseErrorHandler);
	}
}
