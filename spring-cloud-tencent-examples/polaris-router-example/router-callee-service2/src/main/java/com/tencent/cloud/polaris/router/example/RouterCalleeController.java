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

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Discovery callee controller.
 *
 * @author lepdou 2022-04-06
 */
@RestController
@RequestMapping("/router/service/callee")
public class RouterCalleeController {

	private static Logger LOG = LoggerFactory.getLogger(RouterCalleeController.class);

	@Value("${server.port:0}")
	private int port;

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@PostMapping("/info")
	public String info(@RequestParam("name") String name, @RequestBody User user) {
		LOG.info("Discovery Service Callee [{}] is called.", port);
		return String.format("Discovery Service Callee [%s] is called. user = %s", port, cleanXSS(user));
	}

	private User cleanXSS(User user) {
		User u = new User();
		String name = ESAPI.encoder().encodeForHTML(user.getName());
		u.setName(name);
		u.setAge(user.getAge());
		return u;
	}
}
