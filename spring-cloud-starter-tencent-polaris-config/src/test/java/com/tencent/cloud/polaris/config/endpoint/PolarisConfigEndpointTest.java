/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.config.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.config.adapter.MockedConfigKVFile;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for polaris config endpoint.
 *
 * @author shuiqingliu
 */
@ExtendWith(MockitoExtension.class)
public class PolarisConfigEndpointTest {

	private final String testNamespace = "testNamespace";
	private final String testServiceName = "testServiceName";
	private final String testFileName = "application.properties";

	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@BeforeEach
	public void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
	}

	@Test
	public void testPolarisConfigEndpoint() {
		Map<String, Object> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("k3", "v3");
		MockedConfigKVFile file = new MockedConfigKVFile(content);
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testServiceName, testFileName,
				file, content);
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		PolarisConfigEndpoint endpoint = new PolarisConfigEndpoint(polarisConfigProperties);
		Map<String, Object> info = endpoint.polarisConfig();
		assertThat(polarisConfigProperties).isEqualTo(info.get("PolarisConfigProperties"));
		assertThat(Collections.singletonList(polarisPropertySource)).isEqualTo(info.get("PolarisPropertySource"));
	}
}
