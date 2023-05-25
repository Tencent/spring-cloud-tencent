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

package com.tencent.cloud.polaris.context;

import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter;
import com.tencent.cloud.polaris.ratelimit.resolver.RateLimitRuleArgumentServletResolver;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException.TooManyRequests;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for rate-limit.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {CalleeControllerTests.Config.class, TestController.class},
		properties = {"spring.application.name=java_provider_test", "spring.cloud.polaris.discovery.namespace=Test",
				"spring.cloud.polaris.address=grpc://127.0.0.1:10081"})
public class CalleeControllerTests {

	private static NamingServer namingServer;

	@LocalServerPort
	private int port;

	@Autowired
	private RestTemplate restTemplate;

	@MockBean
	private LimitAPI limitAPI;

	@BeforeAll
	static void beforeAll() throws Exception {
		namingServer = NamingServer.startNamingServer(10081);

		// add service with 3 instances
		NamingService.InstanceParameter instanceParameter = new NamingService.InstanceParameter();
		instanceParameter.setHealthy(true);
		instanceParameter.setIsolated(false);
		instanceParameter.setWeight(100);
		ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER);
		namingServer.getNamingService().batchAddInstances(serviceKey, PORT, 3, instanceParameter);
	}

	@AfterAll
	static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@BeforeEach
	void setUp() {
		QuotaResponse quotaResponse = mock(QuotaResponse.class);
		when(quotaResponse.getCode()).thenReturn(QuotaResultCode.QuotaResultOk);
		when(limitAPI.getQuota(any())).thenReturn(quotaResponse);
	}

	@Test
	public void test1() {
		String url = "http://localhost:" + port + "/test/info";

		boolean hasPassed = false;
		boolean hasLimited = false;
		for (int i = 0; i < 30; i++) {
			try {
				if (i > 9) {
					QuotaResponse quotaResponse = mock(QuotaResponse.class);
					when(quotaResponse.getCode()).thenReturn(QuotaResultCode.QuotaResultLimited);
					when(quotaResponse.getInfo()).thenReturn("Testing rate limit after 10 times success.");
					when(limitAPI.getQuota(any())).thenReturn(quotaResponse);
				}
				String result = restTemplate.getForObject(url, String.class);
				System.out.println(result + " [" + i + "]");
				hasPassed = true;
			}
			catch (RestClientException e) {
				if (e instanceof TooManyRequests) {
					System.out.println(((TooManyRequests) e).getResponseBodyAsString());
					hasLimited = true;
				}
				else {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		}
		assertThat(hasPassed).isTrue();
		assertThat(hasLimited).isTrue();
	}

	@Configuration
	@EnableAutoConfiguration
	public static class Config {

		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@Bean
		public LimitAPI limitAPI(PolarisSDKContextManager polarisSDKContextManager) {
			return LimitAPIFactory.createLimitAPIByContext(polarisSDKContextManager.getSDKContext());
		}

		@Bean
		@Primary
		public QuotaCheckServletFilter quotaCheckFilter(LimitAPI limitAPI,
				PolarisRateLimitProperties polarisRateLimitProperties,
				RateLimitRuleArgumentServletResolver rateLimitRuleArgumentResolver,
				@Autowired(required = false) PolarisRateLimiterLimitedFallback polarisRateLimiterLimitedFallback) {
			return new QuotaCheckServletFilter(limitAPI, polarisRateLimitProperties,
					rateLimitRuleArgumentResolver, polarisRateLimiterLimitedFallback);
		}
	}
}
