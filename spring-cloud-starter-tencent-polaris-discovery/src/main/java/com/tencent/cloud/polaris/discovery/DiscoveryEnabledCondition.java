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

package com.tencent.cloud.polaris.discovery;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition for checking if discovery enabled.
 *
 * @author Haotian Zhang
 */
public class DiscoveryEnabledCondition implements Condition {

	@Override
	public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {

		boolean isDiscoveryEnabled = Boolean.parseBoolean(
				conditionContext.getEnvironment().getProperty("spring.cloud.polaris.discovery.enabled", "true"));

		boolean isConsulDiscoveryEnabled = Boolean.parseBoolean(
				conditionContext.getEnvironment().getProperty("spring.cloud.consul.enabled", "false"))
				&& Boolean.parseBoolean(
				conditionContext.getEnvironment().getProperty("spring.cloud.consul.discovery.enabled", "true"));

		isDiscoveryEnabled |= isConsulDiscoveryEnabled;

		boolean isNacosDiscoveryEnabled = Boolean.parseBoolean(
				conditionContext.getEnvironment().getProperty("spring.cloud.nacos.enabled", "false"))
				&& Boolean.parseBoolean(
				conditionContext.getEnvironment().getProperty("spring.cloud.nacos.discovery.enabled", "true"));

		isDiscoveryEnabled |= isNacosDiscoveryEnabled;

		return isDiscoveryEnabled;
	}
}
