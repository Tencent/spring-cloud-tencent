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

import com.tencent.polaris.plugins.router.lane.BaseLaneMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * the configuration for lane router.
 *
 * @author Yuwei Fu
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.router.lane-router")
public class PolarisLaneRouterProperties {

	private BaseLaneMode baseLaneMode = BaseLaneMode.ONLY_UNTAGGED_INSTANCE;

	public BaseLaneMode getBaseLaneMode() {
		return baseLaneMode;
	}

	public void setBaseLaneMode(BaseLaneMode baseLaneMode) {
		this.baseLaneMode = baseLaneMode;
	}

	@Override
	public String toString() {
		return "PolarisLaneRouterProperties{" +
				"baseLaneMode=" + baseLaneMode +
				'}';
	}
}
