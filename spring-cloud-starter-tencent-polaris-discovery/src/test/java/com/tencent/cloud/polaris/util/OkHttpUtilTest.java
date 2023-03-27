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

package com.tencent.cloud.polaris.util;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link OkHttpUtil}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OkHttpUtilTest.TestApplication.class, properties = {"spring.application.name=test", "spring.cloud.polaris.discovery.register=false"})
public class OkHttpUtilTest {

	@LocalServerPort
	private int port;

	@Test
	public void testGet() {
		assertThat(OkHttpUtil.get("http://localhost:" + port + "/test", Maps.newHashMap("key", "value"))).isTrue();
		assertThat(OkHttpUtil.get("http://localhost:" + port + "/error", Maps.newHashMap("key", "value"))).isFalse();
		assertThat(OkHttpUtil.get("http://localhost:55555/error", Maps.newHashMap("key", "value"))).isFalse();
	}

	@SpringBootApplication
	@RestController
	static class TestApplication {
		@GetMapping("/test")
		public String test(@RequestHeader("key") String key) {
			return key;
		}
	}
}
