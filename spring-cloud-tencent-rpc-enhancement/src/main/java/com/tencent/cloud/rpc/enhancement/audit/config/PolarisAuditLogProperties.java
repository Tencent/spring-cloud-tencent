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

package com.tencent.cloud.rpc.enhancement.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Polaris service call audit logging.
 *
 * @author Yuwei Fu
 */
@ConfigurationProperties("spring.cloud.polaris.audit-log")
public class PolarisAuditLogProperties {

	/**
	 * Whether service call audit logging is enabled.
	 */
	private boolean enabled = false;

	/**
	 * Audit log output format.
	 */
	private String format = "json";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}
}
