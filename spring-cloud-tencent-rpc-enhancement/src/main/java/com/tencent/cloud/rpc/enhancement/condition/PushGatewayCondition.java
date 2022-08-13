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

package com.tencent.cloud.rpc.enhancement.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author lingxiao.wlx
 */
public class PushGatewayCondition extends SpringBootCondition {

	private static final String POLARIS_STAT_PUSH_GATEWAY_ENABLED = "spring.cloud.polaris.stat.pushgateway.enabled";

	private static final String POLARIS_STAT_ENABLED = "spring.cloud.polaris.stat.enabled";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		Boolean statEnabled = environment.getProperty(POLARIS_STAT_ENABLED, Boolean.class, false);
		Boolean statPushGatewayEnabled = environment.getProperty(POLARIS_STAT_PUSH_GATEWAY_ENABLED, Boolean.class, false);
		if (statEnabled && statPushGatewayEnabled) {
			return ConditionOutcome.match("matched");
		}
		return ConditionOutcome.noMatch("matched");
	}
}
