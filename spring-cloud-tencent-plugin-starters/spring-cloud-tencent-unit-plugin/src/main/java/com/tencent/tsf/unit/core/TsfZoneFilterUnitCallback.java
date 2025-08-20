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

package com.tencent.tsf.unit.core;

import java.util.Objects;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.zonefilter.TsfZoneFilterManager;

public class TsfZoneFilterUnitCallback implements IUnitChangeCallback {

	@Override
	public void callback() {
		TsfZoneFilterManager.setDisabledZoneSet(TencentUnitManager.getDisableZoneSet());
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TsfZoneFilterUnitCallback)) {
			return false;
		}
		TsfZoneFilterUnitCallback that = (TsfZoneFilterUnitCallback) o;
		return StringUtils.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}
}
