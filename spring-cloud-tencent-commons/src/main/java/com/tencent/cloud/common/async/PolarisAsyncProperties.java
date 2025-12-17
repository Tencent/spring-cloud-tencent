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

package com.tencent.cloud.common.async;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Metadata Properties from local properties file.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.tencent.async")
public class PolarisAsyncProperties {

	/**
	 * Enable async or not.
	 */
	private Boolean enabled = false;

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "PolarisAsyncProperties{" +
				"enabled=" + enabled +
				'}';
	}
}
