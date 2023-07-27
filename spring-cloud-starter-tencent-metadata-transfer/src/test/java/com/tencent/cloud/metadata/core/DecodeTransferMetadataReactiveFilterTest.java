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

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

/**
 * Test for {@link DecodeTransferMetadataReactiveFilter}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = MOCK,
		classes = DecodeTransferMetadataServletFilterTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml"})
public class DecodeTransferMetadataReactiveFilterTest {

	@Autowired
	private MetadataLocalProperties metadataLocalProperties;

	private DecodeTransferMetadataReactiveFilter metadataReactiveFilter;

	@BeforeEach
	public void setUp() {
		this.metadataReactiveFilter = new DecodeTransferMetadataReactiveFilter();
	}

	@Test
	public void test1() {
		assertThat(this.metadataReactiveFilter.getOrder())
				.isEqualTo(OrderConstant.Server.Reactive.DECODE_TRANSFER_METADATA_FILTER_ORDER);
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
		assertThat(metadataLocalProperties.getContent().get("a")).isEqualTo("1");
		assertThat(metadataLocalProperties.getContent().get("b")).isEqualTo("2");
		assertThat(metadataLocalProperties.getContent().get("c")).isNull();
	}
}
