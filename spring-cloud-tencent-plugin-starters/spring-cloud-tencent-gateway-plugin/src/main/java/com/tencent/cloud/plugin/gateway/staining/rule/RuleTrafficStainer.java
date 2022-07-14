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
import java.util.Map;

import com.tencent.cloud.plugin.gateway.staining.TrafficStainer;

import org.springframework.web.server.ServerWebExchange;

/**
 * Staining the request by staining rules.
 * @author lepdou 2022-07-06
 */
public class RuleTrafficStainer implements TrafficStainer {

	private final StainingRuleManager stainingRuleManager;
	private final RuleStainingExecutor ruleStainingExecutor;

	public RuleTrafficStainer(StainingRuleManager stainingRuleManager, RuleStainingExecutor ruleStainingExecutor) {
		this.stainingRuleManager = stainingRuleManager;
		this.ruleStainingExecutor = ruleStainingExecutor;
	}

	@Override
	public Map<String, String> apply(ServerWebExchange exchange) {
		StainingRule stainingRule = stainingRuleManager.getStainingRule();

		if (stainingRule == null) {
			return Collections.emptyMap();
		}

		return ruleStainingExecutor.execute(exchange, stainingRule);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
