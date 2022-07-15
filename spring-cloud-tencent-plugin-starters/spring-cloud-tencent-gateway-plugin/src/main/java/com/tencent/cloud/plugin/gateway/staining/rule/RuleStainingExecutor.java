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

package com.tencent.cloud.plugin.gateway.staining.rule;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.rule.Condition;
import com.tencent.cloud.common.rule.ConditionUtils;
import com.tencent.cloud.common.rule.KVPairUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;

import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolve labels from request by staining rule.
 * @author lepdou 2022-07-11
 */
public class RuleStainingExecutor {

	Map<String, String> execute(ServerWebExchange exchange, StainingRule stainingRule) {
		if (stainingRule == null) {
			return Collections.emptyMap();
		}

		List<StainingRule.Rule> rules = stainingRule.getRules();
		if (CollectionUtils.isEmpty(rules)) {
			return Collections.emptyMap();
		}

		Map<String, String> parsedLabels = new HashMap<>();

		for (StainingRule.Rule rule : rules) {
			List<Condition> conditions = rule.getConditions();

			Set<String> keys = new HashSet<>();
			conditions.forEach(condition -> keys.add(condition.getKey()));
			Map<String, String> actualValues = SpringWebExpressionLabelUtils.resolve(exchange, keys);

			if (!ConditionUtils.match(actualValues, conditions)) {
				continue;
			}

			parsedLabels.putAll(KVPairUtils.toMap(rule.getLabels()));
		}

		return parsedLabels;
	}
}
