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

package com.tencent.cloud.polaris.config.spring.event;

import java.util.Map;
import java.util.Set;

import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;

import org.springframework.context.ApplicationEvent;

/**
 * A spring change event when config is changed.
 *
 * @author Derek Yi 2022-10-16
 */
public class ConfigChangeSpringEvent extends ApplicationEvent {
	/**
	 * @param source all changed keys map.
	 */
	public ConfigChangeSpringEvent(Map<String, ConfigPropertyChangeInfo> source) {
		super(source);
	}

	/**
	 * Get the changed keys.
	 * @return the list of the keys
	 */
	public Set<String> changedKeys() {
		return changeMap().keySet();
	}

	/**
	 * Get a specific change instance for the key specified.
	 * @param key the changed key
	 * @return the change instance
	 */
	public ConfigPropertyChangeInfo getChange(String key) {
		return changeMap().get(key);
	}

	/**
	 * Check whether the specified key is changed .
	 * @param key the key
	 * @return true if the key is changed, false otherwise.
	 */
	public boolean isChanged(String key) {
		return changeMap().containsKey(key);
	}

	private Map<String, ConfigPropertyChangeInfo> changeMap() {
		return (Map<String, ConfigPropertyChangeInfo>) getSource();
	}
}
