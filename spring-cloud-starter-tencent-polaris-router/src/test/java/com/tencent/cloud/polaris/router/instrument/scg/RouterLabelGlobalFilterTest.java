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

package com.tencent.cloud.polaris.router.instrument.scg;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;

/**
 * Test for ${@link RouterLabelGlobalFilter}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RouterLabelGlobalFilterTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=test", "spring.application.name=test", "spring.main.web-application-type=reactive", "spring.cloud.gateway.mvc.enabled=false"})
public class RouterLabelGlobalFilterTest {

	@Test
	public void testRouterLabel() {
		RouterLabelGlobalFilter routerLabelGlobalFilter = new RouterLabelGlobalFilter();

		assertThat(routerLabelGlobalFilter.getOrder())
				.isEqualTo(LOAD_BALANCER_CLIENT_FILTER_ORDER - 1);

		MockServerHttpRequest request = MockServerHttpRequest.post("/test/path")
				.header("uid", "1000")
				.cookie(new HttpCookie("k1", "v1"))
				.queryParam("q1", "a1")
				.build();
		MockServerWebExchange mockWebExchange = new MockServerWebExchange.Builder(request).build();

		routerLabelGlobalFilter.filter(mockWebExchange, new EmptyGatewayFilterChain());

		// get message metadata container
		MetadataContainer metadataContainer = MetadataContextHolder.get()
				.getMetadataContainer(MetadataType.MESSAGE, false);
		// method
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo(Request.HttpMethod.POST.toString());
		// path
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/test/path");
		// header
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, "uid")).isEqualTo("1000");
		// cookie
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, "k1")).isEqualTo("v1");
		// query
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, "q1")).isEqualTo("a1");
	}

	static class EmptyGatewayFilterChain implements GatewayFilterChain {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			return Mono.empty();
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
