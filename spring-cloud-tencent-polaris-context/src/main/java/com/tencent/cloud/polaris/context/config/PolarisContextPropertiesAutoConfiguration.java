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

package com.tencent.cloud.polaris.context.config;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.polaris.client.api.SDKContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Polaris {@link SDKContext}.
 *
 * @author Haotian Zhang
 */
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties
public class PolarisContextPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisContextProperties polarisContextProperties() {
		return new PolarisContextProperties();
	}
}
