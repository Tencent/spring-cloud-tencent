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

package com.tencent.cloud.metadata.config;

import com.tencent.cloud.metadata.core.interceptor.Metadata2HeaderFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.Metadata2HeaderRestTemplateInterceptor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.filter.GlobalFilter;

/**
 * Test for {@link MetadataTransferAutoConfiguration}
 *
 * @author Haotian Zhang
 */
public class MetadataTransferAutoConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	/**
	 * No any web application.
	 */
	@Test
	public void test1() {
		this.applicationContextRunner.withConfiguration(AutoConfigurations.of(MetadataTransferAutoConfiguration.class))
				.run(context -> {
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferFeignInterceptorConfig.class);
					Assertions.assertThat(context).hasSingleBean(Metadata2HeaderFeignInterceptor.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferRestTemplateConfig.class);
					Assertions.assertThat(context).hasSingleBean(Metadata2HeaderRestTemplateInterceptor.class);
					Assertions.assertThat(context).hasSingleBean(
							MetadataTransferAutoConfiguration.MetadataTransferRestTemplateConfig.Metadata2HeaderRestTemplatePostProcessor.class);
					Assertions.assertThat(context)
							.hasSingleBean(MetadataTransferAutoConfiguration.MetadataTransferScgFilterConfig.class);
					Assertions.assertThat(context).hasSingleBean(GlobalFilter.class);
				});
	}

}
