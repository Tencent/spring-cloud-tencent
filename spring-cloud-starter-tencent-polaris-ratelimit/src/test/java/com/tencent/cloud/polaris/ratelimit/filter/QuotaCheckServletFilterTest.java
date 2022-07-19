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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.cloud.polaris.ratelimit.RateLimitRuleLabelResolver;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuotaCheckServletFilter}.
 *
 * @author Haotian Zhang, cheese8
 */
@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = QuotaCheckServletFilterTest.TestApplication.class, properties = {
		"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"
})
public class QuotaCheckServletFilterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<SpringWebExpressionLabelUtils> expressionLabelUtilsMockedStatic;
	private PolarisRateLimiterLabelServletResolver labelResolver =
			exchange -> Collections.singletonMap("ServletResolver", "ServletResolver");
	private QuotaCheckServletFilter quotaCheckServletFilter;
	private QuotaCheckServletFilter quotaCheckWithHtmlRejectTipsServletFilter;

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

		PolarisRateLimitProperties polarisRateLimitWithHtmlRejectTipsProperties = new PolarisRateLimitProperties();
		polarisRateLimitWithHtmlRejectTipsProperties.setRejectRequestTips("<h1>RejectRequestTips提示消息</h1>");
		polarisRateLimitWithHtmlRejectTipsProperties.setRejectHttpCode(419);

		RateLimitRuleLabelResolver rateLimitRuleLabelResolver = mock(RateLimitRuleLabelResolver.class);
		when(rateLimitRuleLabelResolver.getExpressionLabelKeys(anyString(), anyString())).thenReturn(Collections.emptySet());

		this.quotaCheckServletFilter = new QuotaCheckServletFilter(limitAPI, labelResolver, polarisRateLimitProperties, rateLimitRuleLabelResolver);
		this.quotaCheckWithHtmlRejectTipsServletFilter = new QuotaCheckServletFilter(
				limitAPI, labelResolver, polarisRateLimitWithHtmlRejectTipsProperties, rateLimitRuleLabelResolver);
	}

	@Test
	public void testInit() {
		quotaCheckServletFilter.init();
		try {
			Field rejectTips = QuotaCheckServletFilter.class.getDeclaredField("rejectTips");
			rejectTips.setAccessible(true);
			assertThat(rejectTips.get(quotaCheckServletFilter)).isEqualTo("RejectRequestTips提示消息");
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			fail("Exception encountered.", e);
		}
		quotaCheckWithHtmlRejectTipsServletFilter.init();
		try {
			Field rejectTips = QuotaCheckServletFilter.class.getDeclaredField("rejectTips");
			rejectTips.setAccessible(true);
			assertThat(rejectTips.get(quotaCheckWithHtmlRejectTipsServletFilter)).isEqualTo("<h1>RejectRequestTips提示消息</h1>");
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void testGetRuleExpressionLabels() {
		try {
			Method getCustomResolvedLabels = QuotaCheckServletFilter.class.getDeclaredMethod("getCustomResolvedLabels", HttpServletRequest.class);
			getCustomResolvedLabels.setAccessible(true);

			// Mock request
			MockHttpServletRequest request = new MockHttpServletRequest();

			// labelResolver != null
			Map<String, String> result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckServletFilter, request);
			assertThat(result.size()).isEqualTo(1);
			assertThat(result.get("ServletResolver")).isEqualTo("ServletResolver");

			// throw exception
			PolarisRateLimiterLabelServletResolver exceptionLabelResolver = request1 -> {
				throw new RuntimeException("Mock exception.");
			};
			quotaCheckServletFilter = new QuotaCheckServletFilter(null, exceptionLabelResolver, null, null);
			result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckServletFilter, request);
			assertThat(result.size()).isEqualTo(0);

			// labelResolver == null
			quotaCheckServletFilter = new QuotaCheckServletFilter(null, null, null, null);
			result = (Map<String, String>) getCustomResolvedLabels.invoke(quotaCheckServletFilter, request);
			assertThat(result.size()).isEqualTo(0);

			getCustomResolvedLabels.setAccessible(false);
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void testDoFilterInternal() {
		// Create mock FilterChain
		FilterChain filterChain = (servletRequest, servletResponse) -> {

		};

		// Mock request
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		quotaCheckServletFilter.init();
		try {
			// Pass
			MetadataContext.LOCAL_SERVICE = "TestApp1";
			quotaCheckServletFilter.doFilterInternal(request, response, filterChain);

			// Unirate waiting 1000ms
			MetadataContext.LOCAL_SERVICE = "TestApp2";
			long startTimestamp = System.currentTimeMillis();
			quotaCheckServletFilter.doFilterInternal(request, response, filterChain);
			assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

			// Rate limited
			MetadataContext.LOCAL_SERVICE = "TestApp3";
			quotaCheckServletFilter.doFilterInternal(request, response, filterChain);
			assertThat(response.getStatus()).isEqualTo(419);
			assertThat(response.getContentAsString()).isEqualTo("RejectRequestTips提示消息");

			quotaCheckWithHtmlRejectTipsServletFilter.doFilterInternal(request, response, filterChain);
			assertThat(response.getStatus()).isEqualTo(419);
			assertThat(response.getContentAsString()).isEqualTo("RejectRequestTips提示消息");


			// Exception
			MetadataContext.LOCAL_SERVICE = "TestApp4";
			quotaCheckServletFilter.doFilterInternal(request, response, filterChain);
		}
		catch (ServletException | IOException e) {
			fail("Exception encountered.", e);
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
