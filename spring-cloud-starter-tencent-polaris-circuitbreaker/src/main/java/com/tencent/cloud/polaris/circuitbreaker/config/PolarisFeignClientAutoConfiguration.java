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

import com.tencent.cloud.polaris.circuitbreaker.feign.PolarisFeignBeanPostProcessor;
import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Configuration for Polaris {@link feign.Feign} which can automatically bring in the call
 * results for reporting.
 *
 * @author Haotian Zhang
 */
@ConditionalOnProperty(value = "spring.cloud.polaris.circuitbreaker.enabled", havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class PolarisFeignClientAutoConfiguration {

	@Bean
	public ConsumerAPI consumerAPI(SDKContext context) {
		return DiscoveryAPIFactory.createConsumerAPIByContext(context);
	}

	@Bean
	@Order(HIGHEST_PRECEDENCE)
	public PolarisFeignBeanPostProcessor polarisFeignBeanPostProcessor(ConsumerAPI consumerAPI) {
		return new PolarisFeignBeanPostProcessor(consumerAPI);
	}

}
