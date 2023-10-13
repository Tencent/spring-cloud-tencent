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

package com.tencent.cloud.polaris.circuitbreaker.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

/**
 * Endpoint of polaris circuit breaker, include circuit breaker rules.
 *
 * @author wenxuan70
 */
@Endpoint(id = "polaris-circuit-breaker")
public class PolarisCircuitBreakerEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisCircuitBreakerEndpoint.class);

	private final ServiceRuleManager serviceRuleManager;

	public PolarisCircuitBreakerEndpoint(ServiceRuleManager serviceRuleManager) {
		this.serviceRuleManager = serviceRuleManager;
	}

	@ReadOperation
	public Map<String, Object> circuitBreaker(@Selector String dstService) {
		List<CircuitBreakerProto.CircuitBreakerRule> rules = serviceRuleManager.getServiceCircuitBreakerRule(
				MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE,
				dstService
		);

		Map<String, Object> polarisCircuitBreakerInfo = new HashMap<>();

		polarisCircuitBreakerInfo.put("namespace", MetadataContext.LOCAL_NAMESPACE);
		polarisCircuitBreakerInfo.put("service", MetadataContext.LOCAL_SERVICE);

		List<Object> circuitBreakerRules = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(rules)) {
			for (CircuitBreakerProto.CircuitBreakerRule rule : rules) {
				circuitBreakerRules.add(parseCircuitBreakerRule(rule));

			}
		}
		polarisCircuitBreakerInfo.put("circuitBreakerRules", circuitBreakerRules);
		return polarisCircuitBreakerInfo;
	}

	private Object parseCircuitBreakerRule(CircuitBreakerProto.CircuitBreakerRule circuitBreakerRule) {
		String ruleJson;
		try {
			ruleJson = JsonFormat.printer().print(circuitBreakerRule);
		}
		catch (InvalidProtocolBufferException e) {
			LOG.error("rule to Json failed. check rule {}.", circuitBreakerRule, e);
			throw new RuntimeException("Json failed.", e);
		}
		return JacksonUtils.deserialize2Map(ruleJson);
	}
}
