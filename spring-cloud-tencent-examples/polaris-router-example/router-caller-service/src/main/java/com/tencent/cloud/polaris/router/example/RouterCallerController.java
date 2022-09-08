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

package com.tencent.cloud.polaris.router.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Discovery caller controller.
 *
 * @author lepdou 2022-04-06
 */
@RestController
@RequestMapping("/router/service/caller")
public class RouterCallerController {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RouterCalleeService routerCalleeService;

	/**
	 * Get info of two value.
	 * @return info
	 */
	@GetMapping("/feign")
	public String feign(@RequestParam String name) {
		User user = new User();
		user.setName(name);
		user.setAge(18);
		return routerCalleeService.info(name, user);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/rest")
	public String rest(@RequestParam String name) {
		User user = new User();
		user.setName(name);
		user.setAge(18);
		return restTemplate.postForObject(
				"http://RouterCalleeService/router/service/callee/info?name={name}", user, String.class, name);
	}

	/**
	 * health check.
	 * @return health check info
	 */
	@GetMapping("/healthCheck")
	public String healthCheck() {
		return "pk ok";
	}
}
