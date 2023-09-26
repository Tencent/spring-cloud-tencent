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

package com.tencent.cloud.polaris.contract.config;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Auto configuration for Polaris contract properties.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
public class PolarisContractPropertiesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisContractProperties polarisContractProperties(@Nullable ExtendedContractProperties extendedContractProperties) {
		return new PolarisContractProperties(extendedContractProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisContractModifier polarisContractModifier(PolarisContractProperties polarisContractProperties) {
		return new PolarisContractModifier(polarisContractProperties);
	}
}
