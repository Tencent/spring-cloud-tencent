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

package com.tencent.cloud.polaris.config.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.polaris.config.PolarisConfigSDKContextManager;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Endpoint of polaris config.
 *
 * @author shuiqingliu
 **/
@Endpoint(id = "polarisconfig")
public class PolarisConfigEndpoint {

	private final PolarisConfigProperties polarisConfigProperties;

	public PolarisConfigEndpoint(PolarisConfigProperties polarisConfigProperties) {
		this.polarisConfigProperties = polarisConfigProperties;
	}

	@ReadOperation
	public Map<String, Object> polarisConfig() {
		Map<String, Object> configInfo = new HashMap<>();
		configInfo.put("PolarisConfigProperties", polarisConfigProperties);

		List<PolarisPropertySource> propertySourceList = PolarisPropertySourceManager.getAllPropertySources();
		configInfo.put("PolarisPropertySource", propertySourceList);

		configInfo.put("ClientId", getClientId());

		return configInfo;
	}

	/**
	 * The config SDK context is created in the config-data phase, so its startup logs may be
	 * dropped before the polaris log appenders are ready. Exposing the client id here gives
	 * tooling a reliable source instead of grepping logs.
	 */
	private String getClientId() {
		try {
			return PolarisConfigSDKContextManager.innerGetConfigSDKContext().getValueContext().getClientId();
		}
		catch (Throwable throwable) {
			return null;
		}
	}
}
