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

package com.tencent.cloud.polaris.ratelimit.resolver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilterTest;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = RateLimitRuleArgumentReactiveResolverTest.TestApplication.class,
		properties = {
				"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"
		})
public class RateLimitRuleArgumentReactiveResolverTest {

	private final PolarisRateLimiterLabelReactiveResolver labelResolver =
			exchange -> Collections.singletonMap("xxx", "xxx");

	private final PolarisRateLimiterLabelReactiveResolver labelResolverEx =
			exchange -> {
				throw new RuntimeException();
			};

	private RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentReactiveResolver1;
	private RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentReactiveResolver2;
	private RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentReactiveResolver3;
	private RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentReactiveResolver4;

	@BeforeEach
	void setUp() throws InvalidProtocolBufferException {
		MetadataContext.LOCAL_NAMESPACE = "TEST";

		ServiceRuleManager serviceRuleManager = mock(ServiceRuleManager.class);

		RateLimitProto.Rule.Builder ratelimitRuleBuilder =  RateLimitProto.Rule.newBuilder();
		InputStream inputStream = QuotaCheckServletFilterTest.class.getClassLoader().getResourceAsStream("ratelimit.json");
		String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining(""));
		JsonFormat.parser().ignoringUnknownFields().merge(json, ratelimitRuleBuilder);
		RateLimitProto.Rule rateLimitRule = ratelimitRuleBuilder.build();
		RateLimitProto.RateLimit rateLimit = RateLimitProto.RateLimit.newBuilder().addRules(rateLimitRule).build();
		when(serviceRuleManager.getServiceRateLimitRule(anyString(), anyString())).thenReturn(rateLimit);

		// normal
		this.rateLimitRuleArgumentReactiveResolver1 = new RateLimitRuleArgumentReactiveResolver(serviceRuleManager, labelResolver);
		// ex
		this.rateLimitRuleArgumentReactiveResolver2 = new RateLimitRuleArgumentReactiveResolver(serviceRuleManager, labelResolverEx);
		// null
		ServiceRuleManager serviceRuleManager1 = mock(ServiceRuleManager.class);
		when(serviceRuleManager1.getServiceRateLimitRule(anyString(), anyString())).thenReturn(null);
		this.rateLimitRuleArgumentReactiveResolver3 = new RateLimitRuleArgumentReactiveResolver(serviceRuleManager1, labelResolver);
		// null 2
		ServiceRuleManager serviceRuleManager2 = mock(ServiceRuleManager.class);
		RateLimitProto.RateLimit rateLimit2 = RateLimitProto.RateLimit.newBuilder().build();
		when(serviceRuleManager2.getServiceRateLimitRule(anyString(), anyString())).thenReturn(rateLimit2);
		this.rateLimitRuleArgumentReactiveResolver4 = new RateLimitRuleArgumentReactiveResolver(serviceRuleManager2, labelResolver);
	}

	@Test
	public void testGetRuleArguments() {
		// Mock request
		MetadataContext.LOCAL_SERVICE = "Test";
		// Mock request
		MockServerHttpRequest request = MockServerHttpRequest.get("http://127.0.0.1:8080/test")
				.remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
				.header("xxx", "xxx")
				.queryParam("yyy", "yyy")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		MetadataContext metadataContext = new MetadataContext();
		metadataContext.setUpstreamDisposableMetadata(new HashMap<String, String>() {{
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, MetadataContext.LOCAL_NAMESPACE);
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAME, MetadataContext.LOCAL_SERVICE);
		}});
		MetadataContextHolder.set(metadataContext);
		Set<Argument> arguments = rateLimitRuleArgumentReactiveResolver1.getArguments(exchange, MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE);
		Set<Argument> exceptRes = new HashSet<>();
		exceptRes.add(Argument.buildMethod("GET"));
		exceptRes.add(Argument.buildHeader("xxx", "xxx"));
		exceptRes.add(Argument.buildQuery("yyy", "yyy"));
		exceptRes.add(Argument.buildCallerIP("127.0.0.1"));
		exceptRes.add(Argument.buildCustom("xxx", "xxx"));
		exceptRes.add(Argument.buildCallerService(MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE));
		assertThat(arguments).isEqualTo(exceptRes);

		rateLimitRuleArgumentReactiveResolver2.getArguments(exchange, MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE);
		rateLimitRuleArgumentReactiveResolver3.getArguments(exchange, MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE);
		rateLimitRuleArgumentReactiveResolver4.getArguments(exchange, MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE);
	}

	@SpringBootApplication
	protected static class TestApplication {
	}

}
