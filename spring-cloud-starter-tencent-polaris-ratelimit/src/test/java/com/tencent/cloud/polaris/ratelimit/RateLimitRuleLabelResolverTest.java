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

package com.tencent.cloud.polaris.ratelimit;

import java.util.Set;

import com.google.protobuf.StringValue;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitRuleLabelResolver}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class RateLimitRuleLabelResolverTest {

	private RateLimitRuleLabelResolver rateLimitRuleLabelResolver;

	@BeforeEach
	void setUp() {
		ServiceRuleManager serviceRuleManager = mock(ServiceRuleManager.class);
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

		rateLimitRuleLabelResolver = new RateLimitRuleLabelResolver(serviceRuleManager);
	}

	@Test
	public void testGetExpressionLabelKeys() {
		// rateLimitRule == null
		String serviceName = "TestApp1";
		Set<String> labelKeys = rateLimitRuleLabelResolver.getExpressionLabelKeys(null, serviceName);
		assertThat(labelKeys).isEmpty();

		// CollectionUtils.isEmpty(rules)
		serviceName = "TestApp2";
		labelKeys = rateLimitRuleLabelResolver.getExpressionLabelKeys(null, serviceName);
		assertThat(labelKeys).isEmpty();

		// CollectionUtils.isEmpty(labels)
		serviceName = "TestApp3";
		labelKeys = rateLimitRuleLabelResolver.getExpressionLabelKeys(null, serviceName);
		assertThat(labelKeys).isEmpty();

		// Has labels
		serviceName = "TestApp4";
		labelKeys = rateLimitRuleLabelResolver.getExpressionLabelKeys(null, serviceName);
		assertThat(labelKeys).isNotEmpty();
		assertThat(labelKeys).contains("${http.method}");
	}
}
