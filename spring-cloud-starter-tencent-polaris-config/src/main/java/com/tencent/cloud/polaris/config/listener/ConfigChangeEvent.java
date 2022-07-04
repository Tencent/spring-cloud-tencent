/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.listener;

import java.util.Map;
import java.util.Set;

import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;

/**
 * A change event when config is changed .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-06-07
 */
public final class ConfigChangeEvent {

	/**
	 * all changes keys map.
	 */
	private final Map<String, ConfigPropertyChangeInfo> changes;

	/**
	 * all interested changed keys.
	 */
	private final Set<String> interestedChangedKeys;

	/**
	 * Config Change Event Constructor.
	 * @param changes all changes keys map
	 * @param interestedChangedKeys all interested changed keys
	 */
	public ConfigChangeEvent(Map<String, ConfigPropertyChangeInfo> changes, Set<String> interestedChangedKeys) {
		this.changes = changes;
		this.interestedChangedKeys = interestedChangedKeys;
	}

	/**
	 * Get the keys changed.
	 * @return the list of the keys
	 */
	public Set<String> changedKeys() {
		return changes.keySet();
	}

	/**
	 * Get a specific change instance for the key specified.
	 * @param key the changed key
	 * @return the change instance
	 */
	public ConfigPropertyChangeInfo getChange(String key) {
		return changes.get(key);
	}

	/**
	 * Check whether the specified key is changed .
	 * @param key the key
	 * @return true if the key is changed, false otherwise.
	 */
	public boolean isChanged(String key) {
		return changes.containsKey(key);
	}

	/**
	 * Maybe subclass override this method.
	 *
	 * @return interested and changed keys
	 */
	public Set<String> interestedChangedKeys() {
		return interestedChangedKeys;
	}
}
