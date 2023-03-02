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

package com.tencent.cloud.common.metadata.endpoint;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for polaris metadata endpoint.
 *
 * @author shuiqingliu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisMetadataEndpointTests {

	@Mock
	private StaticMetadataManager staticMetadataManager;

	@Test
	public void testPolarisMetadataEndpoint() {
		Map<String, String> envMetadata = new HashMap<>();
		envMetadata.put("k1", "v1");
		envMetadata.put("k2", "v2");
		envMetadata.put("k3", "v3");

		when(staticMetadataManager.getAllEnvMetadata()).thenReturn(envMetadata);

		PolarisMetadataEndpoint polarisMetadataEndpoint = new PolarisMetadataEndpoint(staticMetadataManager);
		Map<String, Object> metaMap = polarisMetadataEndpoint.metadata();
		assertThat(envMetadata).isEqualTo(metaMap.get("Env"));
	}
}
