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

package com.tencent.cloud.polaris.config.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
@Endpoint(id = "polaris-config")
public class PolarisConfigEndpoint {

	private final PolarisConfigProperties polarisConfigProperties;
	private final PolarisPropertySourceManager polarisPropertySourceManager;

	public PolarisConfigEndpoint(PolarisConfigProperties polarisConfigProperties, PolarisPropertySourceManager polarisPropertySourceManager) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
	}

	@ReadOperation
	public Map<String, Object> polarisConfig() {
		Map<String, Object> configInfo = new HashMap<>();
		configInfo.put("PolarisConfigProperties", polarisConfigProperties);

		List<PolarisPropertySource> propertySourceList = polarisPropertySourceManager.getAllPropertySources();
		configInfo.put("PolarisPropertySource", propertySourceList);

		return configInfo;
	}
}
