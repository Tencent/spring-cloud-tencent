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
 *
 */

package com.tencent.cloud.polaris.ratelimit;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

import org.springframework.util.CollectionUtils;

/**
 * resolve labels from rate limit rule.
 *
 *@author lepdou 2022-05-13
 */
@Deprecated
public class RateLimitRuleLabelResolver {

	private final ServiceRuleManager serviceRuleManager;

	public RateLimitRuleLabelResolver(ServiceRuleManager serviceRuleManager) {
		this.serviceRuleManager = serviceRuleManager;
	}

	public Set<String> getExpressionLabelKeys(String namespace, String service) {
		RateLimitProto.RateLimit rateLimitRule = serviceRuleManager.getServiceRateLimitRule(namespace, service);
		if (rateLimitRule == null) {
			return Collections.emptySet();
		}

		List<RateLimitProto.Rule> rules = rateLimitRule.getRulesList();
		if (CollectionUtils.isEmpty(rules)) {
			return Collections.emptySet();
		}

		Set<String> expressionLabels = new HashSet<>();
		for (RateLimitProto.Rule rule : rules) {
			Map<String, ModelProto.MatchString> labels = rule.getLabelsMap();
			if (CollectionUtils.isEmpty(labels)) {
				return Collections.emptySet();
			}
			for (String key : labels.keySet()) {
				if (ExpressionLabelUtils.isExpressionLabel(key)) {
					expressionLabels.add(key);
				}
			}
		}
		return expressionLabels;
	}
}
