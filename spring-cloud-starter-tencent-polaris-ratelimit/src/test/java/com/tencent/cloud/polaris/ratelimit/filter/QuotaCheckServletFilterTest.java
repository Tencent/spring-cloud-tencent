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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentServletResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuotaCheckServletFilter}.
 *
 * @author Haotian Zhang, cheese8
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = QuotaCheckServletFilterTest.TestApplication.class,
		properties = {
		"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"
})
public class QuotaCheckServletFilterTest {

	private final PolarisRateLimiterLabelServletResolver labelResolver =
			exchange -> Collections.singletonMap("xxx", "xxx");
	private QuotaCheckServletFilter quotaCheckServletFilter;
	private QuotaCheckServletFilter quotaCheckWithHtmlRejectTipsServletFilter;
	private QuotaCheckServletFilter quotaCheckWithRateLimiterLimitedFallbackFilter;
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

		RateLimitProto.Rule.Builder ratelimitRuleBuilder =  RateLimitProto.Rule.newBuilder();
		InputStream inputStream = QuotaCheckServletFilterTest.class.getClassLoader().getResourceAsStream("ratelimit.json");
		String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining(""));
		JsonFormat.parser().ignoringUnknownFields().merge(json, ratelimitRuleBuilder);
		RateLimitProto.Rule rateLimitRule = ratelimitRuleBuilder.build();
		RateLimitProto.RateLimit rateLimit = RateLimitProto.RateLimit.newBuilder().addRules(rateLimitRule).build();
		when(serviceRuleManager.getServiceRateLimitRule(anyString(), anyString())).thenReturn(rateLimit);

		RateLimitRuleArgumentServletResolver rateLimitRuleArgumentServletResolver = new RateLimitRuleArgumentServletResolver(serviceRuleManager, labelResolver);
		this.quotaCheckServletFilter = new QuotaCheckServletFilter(limitAPI, polarisRateLimitProperties, rateLimitRuleArgumentServletResolver, null);
		this.quotaCheckWithHtmlRejectTipsServletFilter = new QuotaCheckServletFilter(limitAPI, polarisRateLimitWithHtmlRejectTipsProperties, rateLimitRuleArgumentServletResolver, null);
		this.polarisRateLimiterLimitedFallback = new JsonPolarisRateLimiterLimitedFallback();
		this.quotaCheckWithRateLimiterLimitedFallbackFilter = new QuotaCheckServletFilter(limitAPI, polarisRateLimitWithHtmlRejectTipsProperties, rateLimitRuleArgumentServletResolver, polarisRateLimiterLimitedFallback);
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
		quotaCheckWithRateLimiterLimitedFallbackFilter.init();
	}

	@Test
	public void testDoFilterInternal() {
		// Create mock FilterChain
		FilterChain filterChain = (servletRequest, servletResponse) -> {

		};

		// Mock request
		MockHttpServletRequest request = new MockHttpServletRequest();

		quotaCheckServletFilter.init();
		quotaCheckWithHtmlRejectTipsServletFilter.init();
		try {
			// Pass
			MetadataContext.LOCAL_SERVICE = "TestApp1";
			MockHttpServletResponse testApp1Response = new MockHttpServletResponse();
			quotaCheckServletFilter.doFilterInternal(request, testApp1Response, filterChain);

			// Unirate waiting 1000ms
			MetadataContext.LOCAL_SERVICE = "TestApp2";
			MockHttpServletResponse testApp2Response = new MockHttpServletResponse();
			long startTimestamp = System.currentTimeMillis();
			quotaCheckServletFilter.doFilterInternal(request, testApp2Response, filterChain);
			assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

			// Rate limited
			MetadataContext.LOCAL_SERVICE = "TestApp3";
			MockHttpServletResponse testApp3Response = new MockHttpServletResponse();
			quotaCheckServletFilter.doFilterInternal(request, testApp3Response, filterChain);
			assertThat(testApp3Response.getStatus()).isEqualTo(419);
			assertThat(testApp3Response.getContentAsString()).isEqualTo("RejectRequestTips提示消息");

			MockHttpServletResponse testApp3Response2 = new MockHttpServletResponse();
			quotaCheckWithHtmlRejectTipsServletFilter.doFilterInternal(request, testApp3Response2, filterChain);
			assertThat(testApp3Response2.getStatus()).isEqualTo(419);
			assertThat(testApp3Response2.getContentAsString()).isEqualTo("<h1>RejectRequestTips提示消息</h1>");

			// Exception
			MockHttpServletResponse testApp4Response = new MockHttpServletResponse();
			MetadataContext.LOCAL_SERVICE = "TestApp4";
			quotaCheckServletFilter.doFilterInternal(request, testApp4Response, filterChain);
		}
		catch (ServletException | IOException e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void polarisRateLimiterLimitedFallbackTest() {
		// Create mock FilterChain
		FilterChain filterChain = (servletRequest, servletResponse) -> {
		};

		// Mock request
		MockHttpServletRequest request = new MockHttpServletRequest();

		quotaCheckWithRateLimiterLimitedFallbackFilter.init();
		try {
			// Pass
			MetadataContext.LOCAL_SERVICE = "TestApp1";
			MockHttpServletResponse testApp1response = new MockHttpServletResponse();
			quotaCheckWithRateLimiterLimitedFallbackFilter.doFilterInternal(request, testApp1response, filterChain);

			// Unirate waiting 1000ms
			MetadataContext.LOCAL_SERVICE = "TestApp2";
			MockHttpServletResponse testApp2response = new MockHttpServletResponse();
			long startTimestamp = System.currentTimeMillis();
			quotaCheckWithRateLimiterLimitedFallbackFilter.doFilterInternal(request, testApp2response, filterChain);
			assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(1000L);

			// Rate limited
			MetadataContext.LOCAL_SERVICE = "TestApp3";
			MockHttpServletResponse testApp3response = new MockHttpServletResponse();
			String contentType = new MediaType(polarisRateLimiterLimitedFallback.mediaType(), polarisRateLimiterLimitedFallback.charset()).toString();
			quotaCheckWithRateLimiterLimitedFallbackFilter.doFilterInternal(request, testApp3response, filterChain);
			assertThat(testApp3response.getStatus()).isEqualTo(polarisRateLimiterLimitedFallback.rejectHttpCode());
			assertThat(testApp3response.getContentAsString()).isEqualTo(polarisRateLimiterLimitedFallback.rejectTips());
			assertThat(testApp3response.getContentType()).isEqualTo(contentType);

			// Exception
			MetadataContext.LOCAL_SERVICE = "TestApp4";
			MockHttpServletResponse testApp4response = new MockHttpServletResponse();
			quotaCheckWithRateLimiterLimitedFallbackFilter.doFilterInternal(request, testApp4response, filterChain);
		}
		catch (ServletException | IOException e) {
			fail("Exception encountered.", e);
		}
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
