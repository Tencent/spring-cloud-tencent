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

package com.tencent.cloud.plugin.faulttolerance.integration;

import com.tencent.cloud.plugin.faulttolerance.config.FaultToleranceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Fault Tolerance.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = FaultToleranceTest.TestApplication.class)
public class FaultToleranceTest {

	@Autowired
	private FaultToleranceService testService;

	@Test
	public void testFailFast() {
		assertThat(testService.failFast()).isEqualTo("fallback");
		assertThat(testService.getFailFastCount()).isEqualTo(1);
	}

	@Test
	public void testFailOver() {
		assertThat(testService.failOver()).isEqualTo("OK");
		assertThat(testService.getFailOverCount()).isEqualTo(4);
	}

	@Test
	public void testForking() {
		assertThat(testService.forking()).isEqualTo("OK");
		assertThat(testService.getForkingCount()).isEqualTo(4);
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration(FaultToleranceAutoConfiguration.class)
	public static class TestApplication {

		@Bean
		public FaultToleranceService faultToleranceService() {
			return new FaultToleranceService();
		}
	}
}
