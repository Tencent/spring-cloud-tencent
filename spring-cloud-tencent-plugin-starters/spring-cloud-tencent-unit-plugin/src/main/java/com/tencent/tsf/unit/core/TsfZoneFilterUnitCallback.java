/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
		if (!(o instanceof TsfZoneFilterUnitCallback that)) {
			return false;
		}
		return StringUtils.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}
}
