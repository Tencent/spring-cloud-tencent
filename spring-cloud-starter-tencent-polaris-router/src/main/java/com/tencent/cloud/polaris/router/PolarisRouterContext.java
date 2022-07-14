/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
 *
 */

package com.tencent.cloud.polaris.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.springframework.util.CollectionUtils;

/**
 * the context for router.
 *
 * @author lepdou 2022-05-17
 */
public class PolarisRouterContext {
	/**
	 * the labels for rule router, contain transitive metadata.
	 */
	public static final String ROUTER_LABELS = "allMetadata";
	/**
	 * transitive labels.
	 */
	public static final String TRANSITIVE_LABELS = "transitiveMetadata";

	private Map<String, Map<String, String>> labels;

	public Map<String, String> getLabels(String labelType) {
		if (CollectionUtils.isEmpty(labels)) {
			return Collections.emptyMap();
		}
		Map<String, String> subLabels = labels.get(labelType);
		if (CollectionUtils.isEmpty(subLabels)) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(subLabels);
	}

	public Map<String, String> getLabels(String labelType, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		Map<String, String> typeLabels = getLabels(labelType);
		if (CollectionUtils.isEmpty(typeLabels)) {
			return Collections.emptyMap();
		}

		Map<String, String> labels = new HashMap<>();
		for (String key : labelKeys) {
			String value = typeLabels.get(key);
			if (StringUtils.isNotBlank(value)) {
				labels.put(key, value);
			}
		}
		return labels;
	}

	public String getLabel(String labelKey) {
		Map<String, String> routerLabels = labels.get(ROUTER_LABELS);
		if (CollectionUtils.isEmpty(routerLabels)) {
			return StringUtils.EMPTY;
		}
		return routerLabels.get(labelKey);
	}

	public Set<String> getLabelAsSet(String labelKey) {
		Map<String, String> routerLabels = labels.get(ROUTER_LABELS);
		if (CollectionUtils.isEmpty(routerLabels)) {
			return Collections.emptySet();
		}

		for (Map.Entry<String, String> entry : routerLabels.entrySet()) {
			if (StringUtils.equalsIgnoreCase(labelKey, entry.getKey())) {
				String keysStr = entry.getValue();
				if (StringUtils.isNotBlank(keysStr)) {
					String[] keysArr = StringUtils.split(keysStr, ",");
					return new HashSet<>(Arrays.asList(keysArr));
				}
			}
		}

		return Collections.emptySet();
	}

	public void putLabels(String labelType, Map<String, String> subLabels) {
		if (this.labels == null) {
			this.labels = new HashMap<>();
		}
		labels.put(labelType, subLabels);
	}
}
