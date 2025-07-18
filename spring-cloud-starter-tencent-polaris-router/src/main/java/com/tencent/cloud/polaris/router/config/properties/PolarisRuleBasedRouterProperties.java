/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.router.config.properties;

import com.tencent.polaris.api.rpc.RuleBasedRouterFailoverType;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * the configuration for rule based router.
 *
 * @author lepdou 2022-05-23
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.router.rule-router")
public class PolarisRuleBasedRouterProperties {

	private boolean enabled = true;

	private RuleBasedRouterFailoverType failOver = RuleBasedRouterFailoverType.all;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public RuleBasedRouterFailoverType getFailOver() {
		return failOver;
	}

	public void setFailOver(RuleBasedRouterFailoverType failOver) {
		this.failOver = failOver;
	}

	@Override
	public String toString() {
		return "PolarisRuleBasedRouterProperties{" +
				"enabled=" + enabled +
				", failOver=" + failOver +
				'}';
	}
}
