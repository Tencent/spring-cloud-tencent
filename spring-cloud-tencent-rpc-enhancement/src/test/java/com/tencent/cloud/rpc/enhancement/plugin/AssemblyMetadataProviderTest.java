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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.rpc.enhancement.plugin.assembly.AssemblyMetadataProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.ServiceInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * AssemblyMetadataProviderTest.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class AssemblyMetadataProviderTest {

	@Test
	public void testAssemblyMetadataProvider() {
		ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);
		Map<String, String> metadata = new HashMap<>() {{
			put("k", "v");
		}};
		doReturn(metadata).when(serviceInstance).getMetadata();
		doReturn("0.0.0.0").when(serviceInstance).getHost();
		doReturn("test").when(serviceInstance).getServiceId();
		AssemblyMetadataProvider assemblyMetadataProvider = new AssemblyMetadataProvider(serviceInstance, "test");
		assertThat(assemblyMetadataProvider.getMetadata("k")).isEqualTo("v");
		assertThat(assemblyMetadataProvider.getLocalIp()).isEqualTo("0.0.0.0");
		assertThat(assemblyMetadataProvider.getLocalService()).isEqualTo(new ServiceKey("test", "test"));
	}
}
