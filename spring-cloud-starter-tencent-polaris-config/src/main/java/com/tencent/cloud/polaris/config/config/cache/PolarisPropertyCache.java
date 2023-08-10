/*
 * Copyright (c) 2023 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */
package com.tencent.cloud.polaris.config.config.cache;

import java.util.HashSet;
import java.util.Set;


/**
 * @author juanyinyang
 * @Date 2023年8月8日 下午4:56:18
 */
public final class PolarisPropertyCache {

	private static final PolarisPropertyCache instance = new PolarisPropertyCache();

	private final Set<String> cache = new HashSet<>();

	private PolarisPropertyCache() {

	}

	public static PolarisPropertyCache getInstance() {
		return instance;
	}

	public Set<String> getCache() {
		return cache;
	}

	public void clear() {
		cache.clear();
	}
}
