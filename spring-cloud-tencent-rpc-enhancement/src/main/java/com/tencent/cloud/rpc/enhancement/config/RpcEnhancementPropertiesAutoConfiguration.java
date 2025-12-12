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

package com.tencent.cloud.rpc.enhancement.config;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Auto Configuration for Polaris {@link feign.Feign} OR {@link RestTemplate} which can automatically bring in the call
 * results for reporting.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer.Xu</a> 2022-06-29
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
public class RpcEnhancementPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RpcEnhancementProperties rpcEnhancementProperties() {
		return new RpcEnhancementProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public RpcEnhancementReporterProperties rpcEnhancementReporterProperties() {
		return new RpcEnhancementReporterProperties();
	}
}
