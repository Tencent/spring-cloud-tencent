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

package com.tencent.cloud.common.metadata.endpoint;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.StaticMetadataManager;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Endpoint of polaris's metadata.
 *
 * @author shuiqingliu
 **/
@Endpoint(id = "polaris-metadata")
public class PolarisMetadataEndpoint {

	private final StaticMetadataManager staticMetadataManager;

	public PolarisMetadataEndpoint(StaticMetadataManager staticMetadataManager) {
		this.staticMetadataManager = staticMetadataManager;
	}

	@ReadOperation
	public Map<String, Object> metadata() {
		Map<String, Object>  result = new HashMap<>();
		result.put("Env", staticMetadataManager.getAllEnvMetadata());
		result.put("EnvTransitive", staticMetadataManager.getEnvTransitiveMetadata());
		result.put("ConfigTransitive", staticMetadataManager.getConfigTransitiveMetadata());
		result.put("ConfigDisposable", staticMetadataManager.getConfigDisposableMetadata());
		result.put("Config", staticMetadataManager.getAllConfigMetadata());
		result.put("MergeStatic", staticMetadataManager.getMergedStaticMetadata());
		result.put("MergeStaticTransitive", staticMetadataManager.getMergedStaticTransitiveMetadata());
		result.put("MergeStaticDisposable", staticMetadataManager.getMergedStaticDisposableMetadata());
		result.put("CustomSPI", staticMetadataManager.getAllCustomMetadata());
		result.put("CustomSPITransitive", staticMetadataManager.getCustomSPITransitiveMetadata());
		result.put("CustomSPIDisposable", staticMetadataManager.getCustomSPIDisposableMetadata());
		result.put("zone", staticMetadataManager.getZone());
		result.put("region", staticMetadataManager.getRegion());
		result.put("campus", staticMetadataManager.getCampus());
		return result;
	}
}
