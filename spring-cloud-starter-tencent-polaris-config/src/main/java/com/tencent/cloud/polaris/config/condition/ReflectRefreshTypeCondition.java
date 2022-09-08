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

package com.tencent.cloud.polaris.config.condition;

import com.tencent.cloud.polaris.config.enums.RefreshType;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The refresh type of reflect condition.
 *
 * @author weihubeats 2022-7-13
 */
public class ReflectRefreshTypeCondition extends SpringBootCondition {

	/**
	 * the property key of refresh type.
	 */
	public static final String POLARIS_CONFIG_REFRESH_TYPE = "spring.cloud.polaris.config.refresh-type";

	private static final RefreshType DEFAULT_REFRESH_TYPE = RefreshType.REFLECT;

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		RefreshType refreshType = context.getEnvironment()
				.getProperty(POLARIS_CONFIG_REFRESH_TYPE, RefreshType.class, DEFAULT_REFRESH_TYPE);

		if (refreshType == RefreshType.REFLECT) {
			return ConditionOutcome.match("matched");
		}

		return ConditionOutcome.noMatch("no matched");
	}
}
