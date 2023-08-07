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
 *
 */

package com.tencent.cloud.polaris.context.config;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.PostInitPolarisSDKContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Post-initialization operations after the application initialization phase is completed.
 *
 * @author lepdou 2022-06-28
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
public class PolarisContextPostConfiguration {

	@Bean
	public PostInitPolarisSDKContext postInitPolarisSDKContext(
			PolarisSDKContextManager polarisSDKContextManager, StaticMetadataManager staticMetadataManager) {
		return new PostInitPolarisSDKContext(polarisSDKContextManager.getSDKContext(), staticMetadataManager);
	}
}
