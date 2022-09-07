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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.polaris.client.pb.RateLimitProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.CollectionUtils;

/**
 * Endpoint of Polaris RateLimit rule.
 *
 * @author shuiqingliu
 **/
@Endpoint(id = "polaris-ratelimit")
public class PolarisRateLimitRuleEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisRateLimitRuleEndpoint.class);

	private final ServiceRuleManager serviceRuleManager;
	private final PolarisRateLimitProperties polarisRateLimitProperties;

	public PolarisRateLimitRuleEndpoint(ServiceRuleManager serviceRuleManager, PolarisRateLimitProperties polarisRateLimitProperties) {
		this.serviceRuleManager = serviceRuleManager;
		this.polarisRateLimitProperties = polarisRateLimitProperties;
	}

	@ReadOperation
	public Map<String, Object> rateLimit() {
		RateLimitProto.RateLimit rateLimit = serviceRuleManager.getServiceRateLimitRule(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE);

		Map<String, Object> result = new HashMap<>();

		result.put("properties", polarisRateLimitProperties);
		result.put("namespace", MetadataContext.LOCAL_NAMESPACE);
		result.put("service", MetadataContext.LOCAL_SERVICE);
		result.put("rateLimitRules", parseRateLimitRule(rateLimit));

		return result;
	}

	private List<Object> parseRateLimitRule(RateLimitProto.RateLimit rateLimit) {
		List<Object> rateLimitRule = new ArrayList<>();
		if (rateLimit == null || CollectionUtils.isEmpty(rateLimit.getRulesList())) {
			return rateLimitRule;
		}

		for (RateLimitProto.Rule rule : rateLimit.getRulesList()) {
			String ruleJson;
			try {
				ruleJson = JsonFormat.printer().print(rule);
			}
			catch (InvalidProtocolBufferException e) {
				LOG.error("rule to Json failed. check rule {}.", rule, e);
				throw new RuntimeException("Json failed.", e);
			}
			rateLimitRule.add(JacksonUtils.deserialize2Map(ruleJson));
		}
		return rateLimitRule;
	}
}
