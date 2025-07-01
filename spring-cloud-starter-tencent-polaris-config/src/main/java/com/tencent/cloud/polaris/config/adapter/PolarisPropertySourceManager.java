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

package com.tencent.cloud.polaris.config.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The manager of polaris property source.
 *
 * @author lepdou 2022-03-28
 */
public final class PolarisPropertySourceManager {

	private static final Map<String, PolarisPropertySource> polarisPropertySources = new ConcurrentHashMap<>();

	private PolarisPropertySourceManager() {
	}

	public static void addPropertySource(PolarisPropertySource polarisPropertySource) {
		polarisPropertySources.put(polarisPropertySource.getPropertySourceName(), polarisPropertySource);
	}

	public static List<PolarisPropertySource> getAllPropertySources() {
		return new ArrayList<>(polarisPropertySources.values());
	}

	/**
	 * Just for test.
	 */
	public static void clearPropertySources() {
		polarisPropertySources.clear();
	}
}
