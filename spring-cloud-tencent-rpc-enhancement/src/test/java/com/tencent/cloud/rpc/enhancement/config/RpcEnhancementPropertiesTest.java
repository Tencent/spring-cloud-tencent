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

package com.tencent.cloud.rpc.enhancement.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test For {@link RpcEnhancementProperties}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RpcEnhancementPropertiesTest.TestApplication.class, properties = {
		"spring.application.name=test",
		"spring.cloud.gateway.enabled=false",
		"spring.cloud.tencent.rpc-enhancement=true",
		"spring.cloud.tencent.rpc-enhancement.ignore-body=false"
})
@ActiveProfiles("test")
public class RpcEnhancementPropertiesTest {

	@Autowired
	private RpcEnhancementProperties rpcEnhancementProperties;

	@Test
	public void testDefaultInitialization() {
		assertThat(rpcEnhancementProperties).isNotNull();
		assertThat(rpcEnhancementProperties.isEnabled()).isTrue();
		assertThat(rpcEnhancementProperties.isIgnoreBody()).isFalse();
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
