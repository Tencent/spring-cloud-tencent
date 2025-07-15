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

package com.tencent.cloud.plugin.trafficmirroring;

import com.tencent.cloud.plugin.trafficmirroring.config.TrafficMirroringProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traffic mirroring exception plugin.
 *
 * @author Haotian Zhang
 */
public class TrafficMirroringExceptionPlugin extends TrafficMirroringPostPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(TrafficMirroringExceptionPlugin.class);

	public TrafficMirroringExceptionPlugin(TrafficMirroringProperties trafficMirroringProperties) {
		super(trafficMirroringProperties);
	}

	@Override
	public String getName() {
		return TrafficMirroringExceptionPlugin.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.EXCEPTION;
	}
}
