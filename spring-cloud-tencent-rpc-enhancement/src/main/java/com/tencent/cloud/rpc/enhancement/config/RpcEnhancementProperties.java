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

package com.tencent.cloud.rpc.enhancement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties of RPC enhancement.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.tencent.rpc-enhancement")
public class RpcEnhancementProperties {

	/**
	 * Whether report call result to polaris.
	 */
	private boolean enabled = true;

	/**
	 * Whether to ignore the body of the request.
	 */
	private boolean ignoreBody = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isIgnoreBody() {
		return ignoreBody;
	}

	public void setIgnoreBody(boolean ignoreBody) {
		this.ignoreBody = ignoreBody;
	}
}
