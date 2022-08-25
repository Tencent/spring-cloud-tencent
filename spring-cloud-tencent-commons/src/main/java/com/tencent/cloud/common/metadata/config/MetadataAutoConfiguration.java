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

package com.tencent.cloud.common.metadata.config;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.spi.InstanceMetadataProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Metadata auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
public class MetadataAutoConfiguration {

	/**
	 * metadata properties.
	 * @return metadata properties
	 */
	@Bean
	public MetadataLocalProperties metadataLocalProperties() {
		return new MetadataLocalProperties();
	}

	@Bean
	public StaticMetadataManager metadataManager(MetadataLocalProperties metadataLocalProperties,
			@Nullable InstanceMetadataProvider instanceMetadataProvider) {
		return new StaticMetadataManager(metadataLocalProperties, instanceMetadataProvider);
	}

}
