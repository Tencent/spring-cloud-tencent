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

package com.tencent.cloud.polaris.ratelimit.filter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuotaCheckReactiveFilter}.
 *
 * @author Haotian Zhang, kaiy
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest(classes = QuotaCheckReactiveFilterTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"})
public class QuotaCheckReactiveFilterTest {
	private final PolarisRateLimiterLabelReactiveResolver labelResolver =
			exchange -> Collections.singletonMap("xxx", "xxx");
	private QuotaCheckReactiveFilter quotaCheckReactiveFilter;
	private QuotaCheckReactiveFilter quotaCheckWithRateLimiterLimitedFallbackReactiveFilter;
	private PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback;

	@BeforeEach
	void setUp() throws InvalidProtocolBufferException {
		MetadataContext.LOCAL_NAMESPACE = "TEST";

		LimitAPI limitAPI = mock(LimitAPI.class);
		when(limitAPI.getQuota(any(QuotaRequest.class))).thenAnswer(invocationOnMock -> {
			String serviceName = ((QuotaRequest) invocationOnMock.getArgument(0)).getService();
			if (serviceName.equals("TestApp1")) {
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "QuotaResultOk"));
			}
			else if (serviceName.equals("TestApp2")) {
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultOk, 1000, "QuotaResultOk"));
			}
			else if (serviceName.equals("TestApp3")) {
				QuotaResponse response = new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, "QuotaResultLimited"));
				response.setActiveRule(RateLimitProto.Rule.newBuilder().build());
				return response;
			}
			else {
				return new QuotaResponse(new QuotaResult(null, 0, null));
			}
		});

		PolarisRateLimitProperties polarisRateLimitProperties = new PolarisRateLimitProperties();
		polarisRateLimitProperties.setRejectRequestTips("RejectRequestTips提示消息");
		polarisRateLimitProperties.setRejectHttpCode(419);

		PolarisRateLimitProperties polarisRateLimitWithHtmlRejectTipsProperties = new PolarisRateLimitProperties();
		polarisRateLimitWithHtmlRejectTipsProperties.setRejectRequestTips("<h1>RejectRequestTips提示消息</h1>");
		polarisRateLimitWithHtmlRejectTipsProperties.setRejectHttpCode(419);

		ServiceRuleManager serviceRuleManager = mock(ServiceRuleManager.class);

		RateLimitProto.Rule.Builder ratelimitRuleBuilder = RateLimitProto.Rule.newBuilder();
		InputStream inputStream = QuotaCheckServletFilterTest.class.getClassLoader()
				.getResourceAsStream("ratelimit.json");
		String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
				.collect(Collectors.joining(""));
		JsonFormat.parser().ignoringUnknownFields().merge(json, ratelimitRuleBuilder);
		RateLimitProto.Rule rateLimitRule = ratelimitRuleBuilder.build();
		RateLimitProto.RateLimit rateLimit = RateLimitProto.RateLimit.newBuilder().addRules(rateLimitRule).build();
		when(serviceRuleManager.getServiceRateLimitRule(anyString(), anyString())).thenReturn(rateLimit);

		RateLimitRuleArgumentReactiveResolver rateLimitRuleArgumentReactiveResolver = new RateLimitRuleArgumentReactiveResolver(serviceRuleManager, labelResolver);
		this.quotaCheckReactiveFilter = new QuotaCheckReactiveFilter(limitAPI, polarisRateLimitProperties, rateLimitRuleArgumentReactiveResolver, null);
		this.polarisRateLimiterLimitedFallback = new JsonPolarisRateLimiterLimitedFallback();
		this.quotaCheckWithRateLimiterLimitedFallbackReactiveFilter = new QuotaCheckReactiveFilter(limitAPI, polarisRateLimitWithHtmlRejectTipsProperties, rateLimitRuleArgumentReactiveResolver, polarisRateLimiterLimitedFallback);
	}

	@Test
	public void testGetOrder() {
		assertThat(this.quotaCheckReactiveFilter.getOrder()).isEqualTo(OrderConstant.Server.Reactive.RATE_LIMIT_FILTER_ORDER);
	}

	@Test
	public void testInit() {
		quotaCheckReactiveFilter.init();
		try {
			Field rejectTips = QuotaCheckReactiveFilter.class.getDeclaredField("rejectTips");
			rejectTips.setAccessible(true);
			assertThat(rejectTips.get(quotaCheckReactiveFilter)).isEqualTo("RejectRequestTips提示消息");
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void testFilter() {
		// Create mock WebFilterChain
		WebFilterChain webFilterChain = serverWebExchange -> Mono.empty();

		// Mock request
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080/test").build();

		quotaCheckReactiveFilter.init();

		// Pass
		MetadataContext.LOCAL_SERVICE = "TestApp1";
		ServerWebExchange testApp1Exchange = MockServerWebExchange.from(request);
		quotaCheckReactiveFilter.filter(testApp1Exchange, webFilterChain);

		// Unirate waiting 1000ms
		MetadataContext.LOCAL_SERVICE = "TestApp2";
		ServerWebExchange testApp2Exchange = MockServerWebExchange.from(request);
		long startTimestamp = System.currentTimeMillis();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		quotaCheckReactiveFilter.filter(testApp2Exchange, webFilterChain).subscribe(e -> {
		}, t -> {
		}, countDownLatch::countDown);
		try {
			countDownLatch.await();
		}
		catch (InterruptedException e) {
			fail("Exception encountered.", e);
		}
		assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

		// Rate limited
		MetadataContext.LOCAL_SERVICE = "TestApp3";
		ServerWebExchange testApp3Exchange = MockServerWebExchange.from(request);
		quotaCheckReactiveFilter.filter(testApp3Exchange, webFilterChain);
		ServerHttpResponse response = testApp3Exchange.getResponse();
		assertThat(response.getRawStatusCode()).isEqualTo(419);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE);

		// Exception
		MetadataContext.LOCAL_SERVICE = "TestApp4";
		ServerWebExchange testApp4Exchange = MockServerWebExchange.from(request);
		quotaCheckReactiveFilter.filter(testApp4Exchange, webFilterChain);
	}

	@Test
	public void polarisRateLimiterLimitedFallbackTest() {
		// Create mock WebFilterChain
		WebFilterChain webFilterChain = serverWebExchange -> Mono.empty();

		// Mock request
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080/test").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		quotaCheckWithRateLimiterLimitedFallbackReactiveFilter.init();

		// Pass
		MetadataContext.LOCAL_SERVICE = "TestApp1";
		quotaCheckWithRateLimiterLimitedFallbackReactiveFilter.filter(exchange, webFilterChain);

		// Unirate waiting 1000ms
		MetadataContext.LOCAL_SERVICE = "TestApp2";
		long startTimestamp = System.currentTimeMillis();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		quotaCheckWithRateLimiterLimitedFallbackReactiveFilter.filter(exchange, webFilterChain).subscribe(e -> {
		}, t -> {
		}, countDownLatch::countDown);
		try {
			countDownLatch.await();
		}
		catch (InterruptedException e) {
			fail("Exception encountered.", e);
		}
		assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

		// Rate limited
		MetadataContext.LOCAL_SERVICE = "TestApp3";
		quotaCheckWithRateLimiterLimitedFallbackReactiveFilter.filter(exchange, webFilterChain);
		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getRawStatusCode()).isEqualTo(polarisRateLimiterLimitedFallback.rejectHttpCode());
		assertThat(response.getHeaders().getContentType()).isEqualTo(polarisRateLimiterLimitedFallback.mediaType());

		// Exception
		MetadataContext.LOCAL_SERVICE = "TestApp4";
		quotaCheckWithRateLimiterLimitedFallbackReactiveFilter.filter(exchange, webFilterChain);
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
