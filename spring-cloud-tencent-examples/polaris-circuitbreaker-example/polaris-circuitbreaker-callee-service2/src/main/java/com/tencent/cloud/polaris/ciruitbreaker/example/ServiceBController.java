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

package com.tencent.cloud.polaris.ciruitbreaker.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service B Controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/example/service/b")
public class ServiceBController {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceBController.class);

	private boolean ifBadGateway = true;

	private boolean ifDelay = true;

	@GetMapping("/setBadGateway")
	public void setBadGateway(@RequestParam boolean param) {
		if (param) {
			LOG.info("info is set to return HttpStatus.BAD_GATEWAY.");
		}
		else {
			LOG.info("info is set to return HttpStatus.OK.");
		}
		this.ifBadGateway = param;
	}

	@GetMapping("/setDelay")
	public void setDelay(@RequestParam boolean param) {
		if (param) {
			LOG.info("info is set to delay 100ms.");
		}
		else {
			LOG.info("info is set to no delay.");
		}
		this.ifDelay = param;
	}

	/**
	 * Get service information.
	 *
	 * @return service information
	 */
	@GetMapping("/info")
	public ResponseEntity<String> info() throws InterruptedException {
		if (ifBadGateway) {
			return new ResponseEntity<>("failed for call my service", HttpStatus.BAD_GATEWAY);
		}
		if (ifDelay) {
			Thread.sleep(100);
		}
		return new ResponseEntity<>("hello world ! I'm a service B2", HttpStatus.OK);
	}
}
