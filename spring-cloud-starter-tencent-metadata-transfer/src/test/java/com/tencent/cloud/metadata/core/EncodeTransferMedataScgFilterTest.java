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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link EncodeTransferMedataScgFilter}.
 *
 * @author quan
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = EncodeTransferMedataScgFilterTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = reactive"})
public class EncodeTransferMedataScgFilterTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Mock
	private GatewayFilterChain chain;

	@Test
	public void testTransitiveMetadataFromApplicationConfig() throws UnsupportedEncodingException {
		EncodeTransferMedataScgFilter filter = applicationContext.getBean(EncodeTransferMedataScgFilter.class);
		MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("");
		MockServerWebExchange exchange = MockServerWebExchange.from(builder);
		filter.filter(exchange, chain);
		String metadataStr = exchange.getRequest().getHeaders().getFirst(MetadataConstant.HeaderName.CUSTOM_METADATA);
		String decode = URLDecoder.decode(metadataStr, UTF_8);
		Map<String, String> transitiveMap = JacksonUtils.deserialize2Map(decode);
		assertThat(transitiveMap.size()).isEqualTo(1);
		assertThat(transitiveMap.get("b")).isEqualTo("2");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
