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

package com.tencent.cloud.polaris.context;

import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SDKContext}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PolarisContextApplication.class,
		properties = {"spring.config.location = classpath:bootstrap.yml"})
@ImportAutoConfiguration({PolarisContextAutoConfiguration.class})
public class PolarisContextGetHostTest {

	@Autowired
	private PolarisSDKContextManager polarisSDKContextManager;

	@Autowired
	private PolarisContextProperties polarisContextProperties;

	@Test
	public void testGetConfigHost() {
		String bindIP = polarisSDKContextManager.getSDKContext().getConfig().getGlobal().getAPI().getBindIP();
		assertThat(StringUtils.isBlank(bindIP)).isFalse();
		assertThat(bindIP).isEqualTo("192.168.1.1");
		assertThat(polarisContextProperties.getAddress()).isEqualTo("grpc://127.0.0.1:8091");
		assertThat(polarisContextProperties.getLocalIpAddress()).isEqualTo("192.168.1.1");
		assertThat(polarisContextProperties.getEnabled()).isTrue();
		assertThat(polarisContextProperties.getNamespace()).isEqualTo("dev");
		assertThat(polarisContextProperties.getService()).isEqualTo("TestApp");
		assertThat(polarisContextProperties.getLocalPort()).isEqualTo(9090);
	}
}
