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

package com.tencent.tsf.unit.core.zonefilter;

import java.util.HashSet;
import java.util.Set;

/**
 * 当前通过从单元化配置解析，后续支持 TSF 管控端下发.
 */
public final class TsfZoneFilterManager {

	private TsfZoneFilterManager() {
	}

	private volatile static Set<String> disabledZoneSet = new HashSet<>();

	public static Set<String> getDisabledZoneSet() {
		return disabledZoneSet;
	}

	public static void setDisabledZoneSet(Set<String> disabledZoneSet) {
		TsfZoneFilterManager.disabledZoneSet = disabledZoneSet;
	}
}
