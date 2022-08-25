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

package com.tencent.cloud.polaris.router.featureenv.feature2backend;

import com.tencent.cloud.common.metadata.StaticMetadataManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lepdou 2022-07-20
 */
@RestController
@RequestMapping("/router")
public class BackendController {

	@Autowired
	private StaticMetadataManager staticMetadataManager;

	@Value("${spring.application.name}")
	private String appName;

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest")
	public String rest() {
		String featureEnv = staticMetadataManager.getMergedStaticMetadata().get("featureenv");
		return appName + "[" + featureEnv + "]";
	}
}
