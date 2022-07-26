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

package com.tencent.cloud.metadata.service.middle;

import java.util.Map;

import com.google.common.collect.Maps;

import org.springframework.stereotype.Component;

/**
 * Metadata callee feign client fallback.
 *
 * @author Palmer Xu
 */
@Component
public class MetadataBackendServiceFallback implements MetadataBackendService {

	@Override
	public Map<String, Map<String, String>> info() {
		return Maps.newHashMap();
	}
}
