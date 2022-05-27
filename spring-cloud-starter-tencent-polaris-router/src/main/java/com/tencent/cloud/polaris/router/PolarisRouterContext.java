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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

/**
 * the context for router.
 *
 *@author lepdou 2022-05-17
 */
public class PolarisRouterContext {

	/**
	 * the label for rule router.
	 */
	public static final String RULE_ROUTER_LABELS = "ruleRouter";
	/**
	 * transitive labels.
	 */
	public static final String TRANSITIVE_LABELS = "transitive";

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

	public void setLabels(String labelType, Map<String, String> subLabels) {
		if (this.labels == null) {
			this.labels = new HashMap<>();
		}
		labels.put(labelType, subLabels);
	}
}
