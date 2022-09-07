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
 */

package com.tencent.cloud.quickstart.callee;

import java.util.Set;

import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;

import org.springframework.stereotype.Component;

/**
 * Custom Config Listener Example .
 *
 * @author Haotian Zhang
 */
@Component
public final class CustomConfigChangeListener {

	/**
	 * PolarisConfigKVFileChangeListener Example .
	 * @param event instance of {@link ConfigChangeEvent}
	 */
	@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = "appName")
	public void onChange(ConfigChangeEvent event) {
		Set<String> changedKeys = event.changedKeys();

		for (String changedKey : changedKeys) {
			System.out.printf("%s = %s \n", changedKey, event.getChange(changedKey));
		}
	}

}
