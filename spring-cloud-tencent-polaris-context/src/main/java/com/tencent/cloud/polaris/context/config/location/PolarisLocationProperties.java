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

package com.tencent.cloud.polaris.context.config.location;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Polaris location provider configuration.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.location")
public class PolarisLocationProperties {

	/**
	 * Cloud location provider configuration.
	 */
	private Cloud cloud = new Cloud();

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(Cloud cloud) {
		this.cloud = cloud;
	}

	@Override
	public String toString() {
		return "PolarisLocationProperties{" +
				"cloud=" + cloud +
				'}';
	}

	/**
	 * Properties for cloud location provider.
	 */
	public static class Cloud {

		/**
		 * Whether to enable the cloud location provider. Default is true.
		 */
		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return "Cloud{" +
					"enabled=" + enabled +
					'}';
		}
	}
}
