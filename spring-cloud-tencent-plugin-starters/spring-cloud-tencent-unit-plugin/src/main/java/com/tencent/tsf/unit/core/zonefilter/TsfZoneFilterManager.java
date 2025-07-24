/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
