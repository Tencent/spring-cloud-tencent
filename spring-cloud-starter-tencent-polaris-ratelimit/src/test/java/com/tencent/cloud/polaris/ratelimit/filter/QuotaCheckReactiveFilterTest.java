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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.cloud.polaris.ratelimit.RateLimitRuleLabelResolver;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelReactiveResolver;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuotaCheckReactiveFilter}.
 *
 * @author Haotian Zhang, cheese8, kaiy
 */
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = QuotaCheckReactiveFilterTest.TestApplication.class, properties = {
		"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"
})
public class QuotaCheckReactiveFilterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<SpringWebExpressionLabelUtils> expressionLabelUtilsMockedStatic;
	private final PolarisRateLimiterLabelReactiveResolver labelResolver =
			exchange -> Collections.singletonMap("ReactiveResolver", "ReactiveResolver");
	private QuotaCheckReactiveFilter quotaCheckReactiveFilter;

	@BeforeClass
	public static void beforeClass() {
		expressionLabelUtilsMockedStatic = mockStatic(SpringWebExpressionLabelUtils.class);
		when(SpringWebExpressionLabelUtils.resolve(any(ServerWebExchange.class), anySet()))
				.thenReturn(Collections.singletonMap("RuleLabelResolver", "RuleLabelResolver"));

		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
		expressionLabelUtilsMockedStatic.close();
	}

	@Before
	public void setUp() {
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
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, "QuotaResultLimited"));
			}
			else {
				return new QuotaResponse(new QuotaResult(null, 0, null));
			}
		});

		PolarisRateLimitProperties polarisRateLimitProperties = new PolarisRateLimitProperties();
		polarisRateLimitProperties.setRejectRequestTips("RejectRequestTips提示消息");
		polarisRateLimitProperties.setRejectHttpCode(419);

		RateLimitRuleLabelResolver rateLimitRuleLabelResolver = mock(RateLimitRuleLabelResolver.class);
		when(rateLimitRuleLabelResolver.getExpressionLabelKeys(anyString(), anyString())).thenReturn(Collections.EMPTY_SET);

		this.quotaCheckReactiveFilter = new QuotaCheckReactiveFilter(
				limitAPI, labelResolver, polarisRateLimitProperties, rateLimitRuleLabelResolver);
	}

	@Test
	public void testGetOrder() {
		assertThat(this.quotaCheckReactiveFilter.getOrder()).isEqualTo(RateLimitConstant.FILTER_ORDER);
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
	public void testGetRuleExpressionLabels() {
		try {
			Method getCustomResolvedLabels = QuotaCheckReactiveFilter.class.getDeclaredMethod("getCustomResolvedLabels", ServerWebExchange.class);
			getCustomResolvedLabels.setAccessible(true);

			// Mock request
			MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080/test").build();
			ServerWebExchange exchange = MockServerWebExchange.from(request);

			// labelResolver != null
			Map<String, String> result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckReactiveFilter, exchange);
			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get("ReactiveResolver")).isEqualTo("ReactiveResolver");

			// throw exception
			PolarisRateLimiterLabelReactiveResolver exceptionLabelResolver = new PolarisRateLimiterLabelReactiveResolver() {
				@Override
				public Map<String, String> resolve(ServerWebExchange exchange) {
					throw new RuntimeException("Mock exception.");
				}
			};
			quotaCheckReactiveFilter = new QuotaCheckReactiveFilter(null, exceptionLabelResolver, null, null);
			result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckReactiveFilter, exchange);
			assertThat(result.size()).isEqualTo(0);

			// labelResolver == null
			quotaCheckReactiveFilter = new QuotaCheckReactiveFilter(null, null, null, null);
			result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckReactiveFilter, exchange);
			assertThat(result.size()).isEqualTo(0);

			getCustomResolvedLabels.setAccessible(false);
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void testFilter() {
		// Create mock WebFilterChain
		WebFilterChain webFilterChain = serverWebExchange -> Mono.empty();

		// Mock request
		MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080/test").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		quotaCheckReactiveFilter.init();

		// Pass
		MetadataContext.LOCAL_SERVICE = "TestApp1";
		quotaCheckReactiveFilter.filter(exchange, webFilterChain);

		// Unirate waiting 1000ms
		MetadataContext.LOCAL_SERVICE = "TestApp2";
		long startTimestamp = System.currentTimeMillis();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		quotaCheckReactiveFilter.filter(exchange, webFilterChain).subscribe(e -> { }, t -> { }, countDownLatch::countDown);
		try {
			countDownLatch.await();
		}
		catch (InterruptedException e) {
			fail("Exception encountered.", e);
		}
		assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

		// Rate limited
		MetadataContext.LOCAL_SERVICE = "TestApp3";
		quotaCheckReactiveFilter.filter(exchange, webFilterChain);
		ServerHttpResponse response = exchange.getResponse();
		assertThat(response.getRawStatusCode()).isEqualTo(419);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE);

		// Exception
		MetadataContext.LOCAL_SERVICE = "TestApp4";
		quotaCheckReactiveFilter.filter(exchange, webFilterChain);
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
