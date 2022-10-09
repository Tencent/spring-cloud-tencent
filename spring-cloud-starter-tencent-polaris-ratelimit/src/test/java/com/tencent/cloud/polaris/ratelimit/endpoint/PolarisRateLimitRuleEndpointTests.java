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

package com.tencent.cloud.polaris.ratelimit.endpoint;

import java.util.Map;

import com.google.protobuf.StringValue;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.polaris.client.pb.ModelProto;
import com.tencent.polaris.client.pb.RateLimitProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for polaris rete limit rule endpoint.
 *
 * @author shuiqingliu
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRateLimitRuleEndpointTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisRateLimitRuleEndpointTests.PolarisRateLimitAutoConfiguration.class,
					PolarisRateLimitRuleEndpointAutoConfiguration.class,
					PolarisRateLimitAutoConfiguration.class,
					PolarisRateLimitAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues(
					"spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private ServiceRuleManager serviceRuleManager;
	private PolarisRateLimitProperties polarisRateLimitProperties;

	@Before
	public void setUp() {
		serviceRuleManager = mock(ServiceRuleManager.class);
		when(serviceRuleManager.getServiceRateLimitRule(any(), anyString())).thenAnswer(invocationOnMock -> {
			String serviceName = invocationOnMock.getArgument(1).toString();
			if (serviceName.equals("TestApp1")) {
				return null;
			}
			else if (serviceName.equals("TestApp2")) {
				return RateLimitProto.RateLimit.newBuilder().build();
			}
			else if (serviceName.equals("TestApp3")) {
				RateLimitProto.Rule rule = RateLimitProto.Rule.newBuilder().build();
				return RateLimitProto.RateLimit.newBuilder().addRules(rule).build();
			}
			else {
				ModelProto.MatchString matchString = ModelProto.MatchString.newBuilder()
						.setType(ModelProto.MatchString.MatchStringType.EXACT)
						.setValue(StringValue.of("value"))
						.setValueType(ModelProto.MatchString.ValueType.TEXT).build();
				RateLimitProto.Rule rule = RateLimitProto.Rule.newBuilder()
						.putLabels("${http.method}", matchString).build();
				return RateLimitProto.RateLimit.newBuilder().addRules(rule).build();
			}
		});
	}

	@Test
	public void testPolarisRateLimit() {
		this.contextRunner.run(context -> polarisRateLimitProperties = context.getBean(PolarisRateLimitProperties.class));
		PolarisRateLimitRuleEndpoint polarisRateLimitRuleEndpoint = new PolarisRateLimitRuleEndpoint(serviceRuleManager, polarisRateLimitProperties);
		Map<String, Object> rateLimit = polarisRateLimitRuleEndpoint.rateLimit();
		assertThat(polarisRateLimitProperties).isEqualTo(rateLimit.get("properties"));
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisRateLimitAutoConfiguration {

	}
}
