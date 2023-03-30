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

package com.tencent.cloud.plugin.discovery.adapter.config;

import java.util.HashMap;

import com.alibaba.cloud.nacos.NacosServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.plugin.discovery.adapter.transformer.NacosInstanceTransformer;
import com.tencent.polaris.api.pojo.Instance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosInstanceTransformerTest.
 *
 * @author sean yu
 */
public class NacosInstanceTransformerTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeAll
	public static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
				.thenReturn("default");
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
				.thenReturn("test");
	}


		@Test
	public void test() {
		NacosInstanceTransformer nacosInstanceTransformer = new NacosInstanceTransformer();
		NacosServiceInstance nacosServiceInstance = new NacosServiceInstance();
		nacosServiceInstance.setMetadata(new HashMap<String, String>() {{
			put("nacos.weight", "0.01");
			put("nacos.healthy", "true");
			put("nacos.instanceId", "xxx");
		}});
		Instance instance = nacosInstanceTransformer.transform(nacosServiceInstance);
		assertThat(instance.isHealthy()).isEqualTo(true);
	}

}
