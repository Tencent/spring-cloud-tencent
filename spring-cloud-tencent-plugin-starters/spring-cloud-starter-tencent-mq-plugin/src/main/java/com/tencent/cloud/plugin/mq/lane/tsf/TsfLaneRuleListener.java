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

package com.tencent.cloud.plugin.mq.lane.tsf;

import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;

public class TsfLaneRuleListener extends AbstractResourceEventListener {

	private final TsfActiveLane tsfActiveLane;

	public TsfLaneRuleListener(TsfActiveLane tsfActiveLane) {
		this.tsfActiveLane = tsfActiveLane;
	}

	@Override
	public void onResourceAdd(ServiceEventKey svcEventKey, RegistryCacheValue newValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LANE_RULE) {
			return;
		}

		tsfActiveLane.freshLaneStatus();
	}

	@Override
	public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue, RegistryCacheValue newValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LANE_RULE) {
			return;
		}

		tsfActiveLane.freshLaneStatus();
	}

	@Override
	public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
		if (svcEventKey.getEventType() != ServiceEventKey.EventType.LANE_RULE) {
			return;
		}

		tsfActiveLane.freshLaneStatus();
	}
}
