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

package com.tencent.cloud.metadata.core;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link EncodeTransferMedataRestTemplateInterceptor}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = EncodeTransferMedataRestTemplateInterceptorTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml"})
public class EncodeTransferMedataRestTemplateInterceptorTest {

	@Autowired
	private RestTemplate restTemplate;

	@LocalServerPort
	private int localServerPort;

	@Test
	public void testTransitiveMetadataFromApplicationConfig() {
		HttpHeaders httpHeaders = new HttpHeaders();
		HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);
		String metadata = restTemplate
				.exchange("http://localhost:" + localServerPort + "/test", HttpMethod.GET, httpEntity, String.class)
				.getBody();
		assertThat(metadata).isEqualTo("2");
	}

	@SpringBootApplication
	@RestController
	protected static class TestApplication {

		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@RequestMapping("/test")
		public String test() {
			return MetadataContextHolder.get().getContext(MetadataContext.FRAGMENT_TRANSITIVE, "b");
		}
	}
}
