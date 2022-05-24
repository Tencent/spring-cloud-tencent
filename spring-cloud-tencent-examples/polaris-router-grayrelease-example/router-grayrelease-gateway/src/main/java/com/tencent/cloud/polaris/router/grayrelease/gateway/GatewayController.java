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

package com.tencent.cloud.polaris.router.grayrelease.gateway;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/router/gray")
public class GatewayController {

	@Autowired
	private Environment environment;

	@Autowired
	private RouterService routerService;

	private final Map<Integer, String> uidMapping;

	public GatewayController() {
		String mapping = System.getenv("uid_env_mapping");
		uidMapping = parseUidMapping(mapping);
		System.out.println("parsed mapping is " + uidMapping);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/route_rule")
	public String routeRule(@RequestHeader("uid") int userId) {
		String appName = environment.getProperty("spring.application.name");
		String resp = routerService.restByUser(userId);
		return appName + " -> " + resp;
	}

	private Map<Integer, String> parseUidMapping(String mapText) {
		Map<Integer, String> result = new HashMap<>();
		if (!StringUtils.hasText(mapText)) {
			return result;
		}
		String[] tokens = mapText.split("\\|");
		for (String token : tokens) {
			String[] pairs = token.split(":");
			result.put(Integer.parseInt(pairs[0]), pairs[1]);
		}
		return result;
	}

}
