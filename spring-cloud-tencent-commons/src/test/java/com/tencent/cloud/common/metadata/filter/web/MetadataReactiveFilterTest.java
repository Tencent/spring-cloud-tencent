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

package com.tencent.cloud.common.metadata.filter.web;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

/**
 * Test for {@link MetadataReactiveFilter}
 *
 * @author Haotian Zhang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = MOCK, classes = MetadataServletFilterTest.TestApplication.class, properties = {
		"spring.config.location = classpath:application-test.yml" })
public class MetadataReactiveFilterTest {

	@Autowired
	private MetadataLocalProperties metadataLocalProperties;

	private MetadataReactiveFilter metadataReactiveFilter;

	@Before
	public void setUp() {
		this.metadataReactiveFilter = new MetadataReactiveFilter();
	}

	@Test
	public void test1() {
		Assertions.assertThat(this.metadataReactiveFilter.getOrder())
				.isEqualTo(MetadataConstant.OrderConstant.WEB_FILTER_ORDER);
	}

	@Test
	public void test2() {
		// Create mock WebFilterChain
		WebFilterChain webFilterChain = serverWebExchange -> Mono.empty();

		// Mock request
		MockServerHttpRequest request = MockServerHttpRequest.get("test")
				.header(MetadataConstant.HeaderName.CUSTOM_METADATA, "{\"c\": \"3\"}")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		metadataReactiveFilter.filter(exchange, webFilterChain);
		Assertions.assertThat(metadataLocalProperties.getContent().get("a"))
				.isEqualTo("1");
		Assertions.assertThat(metadataLocalProperties.getContent().get("b"))
				.isEqualTo("2");
		Assertions.assertThat(metadataLocalProperties.getContent().get("c")).isNull();
	}

}
