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

package com.tencent.cloud.multiple.discovery.nacos;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service B Controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/example/service/b")
public class NacosCalleeServiceBController {

	/**
	 * Get service information.
	 *
	 * @return service information
	 */
	@GetMapping("/info")
	@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
	public String info() {
		return "BAD_GATEWAY ! from service B2";
	}

	@GetMapping("/health")
	@ResponseStatus(value = HttpStatus.BAD_GATEWAY, reason = "failed for call my service")
	public String health() {
		System.out.println("health check: 502 instance");
		return "hello world ! I'm a service B1";
	}

}
