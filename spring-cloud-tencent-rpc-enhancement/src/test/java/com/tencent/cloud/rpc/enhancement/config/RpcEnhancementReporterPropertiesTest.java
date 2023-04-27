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

package com.tencent.cloud.rpc.enhancement.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.MULTIPLE_CHOICES;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

/**
 * Test For {@link RpcEnhancementReporterProperties}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RpcEnhancementReporterPropertiesTest.TestApplication.class, properties = {
		"spring.application.name=test",
		"spring.cloud.gateway.enabled=false",
		"spring.cloud.tencent.rpc-enhancement.reporter=true"
})
@ActiveProfiles("test")
public class RpcEnhancementReporterPropertiesTest {

	@Autowired
	private RpcEnhancementReporterProperties rpcEnhancementReporterProperties;

	@Test
	public void testDefaultInitialization() {
		assertThat(rpcEnhancementReporterProperties).isNotNull();
		assertThat(rpcEnhancementReporterProperties.isIgnoreInternalServerError()).isFalse();
		assertThat(rpcEnhancementReporterProperties.getSeries()).isNotEmpty();
		assertThat(rpcEnhancementReporterProperties.getSeries().get(0)).isEqualTo(CLIENT_ERROR);
		assertThat(rpcEnhancementReporterProperties.getSeries().get(1)).isEqualTo(SERVER_ERROR);
		assertThat(rpcEnhancementReporterProperties.getStatuses()).isNotEmpty();
		assertThat(rpcEnhancementReporterProperties.getStatuses().get(0)).isEqualTo(MULTIPLE_CHOICES);
		assertThat(rpcEnhancementReporterProperties.getStatuses().get(1)).isEqualTo(MOVED_PERMANENTLY);
		assertThat(rpcEnhancementReporterProperties.isEnabled()).isEqualTo(true);
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
