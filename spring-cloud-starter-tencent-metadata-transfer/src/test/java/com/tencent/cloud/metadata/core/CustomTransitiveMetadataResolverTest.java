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

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * Test for {@link CustomTransitiveMetadataResolver}.
 *
 * @author quan
 */
public class CustomTransitiveMetadataResolverTest {

	@Test
	public void testSCTTransitiveMetadata() {
		MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("");
		builder.header("X-SCT-Metadata-Transitive-a", "test");
		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		Map<String, String> resolve = CustomTransitiveMetadataResolver.resolve(exchange);
		Assertions.assertThat(resolve.size()).isEqualTo(1);
		Assertions.assertThat(resolve.get("a")).isEqualTo("test");
	}

	@Test
	public void testPolarisTransitiveMetadata() {
		MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("");
		builder.header("X-Polaris-Metadata-Transitive-a", "test");
		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		Map<String, String> resolve = CustomTransitiveMetadataResolver.resolve(exchange);
		Assertions.assertThat(resolve.size()).isEqualTo(1);
		Assertions.assertThat(resolve.get("a")).isEqualTo("test");
	}

	@Test
	public void testSCTServletTransitiveMetadata() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-SCT-Metadata-Transitive-a", "test");
		Map<String, String> resolve = CustomTransitiveMetadataResolver.resolve(request);
		Assertions.assertThat(resolve.size()).isEqualTo(1);
		Assertions.assertThat(resolve.get("a")).isEqualTo("test");
	}

	@Test
	public void testPolarisServletTransitiveMetadata() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Polaris-Metadata-Transitive-a", "test");
		Map<String, String> resolve = CustomTransitiveMetadataResolver.resolve(request);
		Assertions.assertThat(resolve.size()).isEqualTo(1);
		Assertions.assertThat(resolve.get("a")).isEqualTo("test");
	}
}
