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

package com.tencent.cloud.quickstart.callee;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.quickstart.callee.config.DataSourceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Quickstart callee controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/quickstart/callee")
public class QuickstartCalleeController {

	private static final Logger LOG = LoggerFactory.getLogger(QuickstartCalleeController.class);

	@Value("${server.port:0}")
	private int port;

	@Value("${spring.cloud.client.ip-address:127.0.0.1}")
	private String ip;

	@Value("${appName:${spring.application.name}}")
	private String appName;

	@Autowired
	private DataSourceProperties dataSourceProperties;

	/**
	 * Get sum of two value.
	 * @param value1 value 1
	 * @param value2 value 2
	 * @return sum
	 */
	@GetMapping("/sum")
	public String sum(@RequestParam int value1, @RequestParam int value2) {
		LOG.info("Quickstart Callee Service [{}:{}] is called and sum is [{}].", ip, port, value1 + value2);
		return String.format("Quickstart Callee Service [%s:%s] is called and sum is [%s].", ip, port, value1 + value2);
	}

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@GetMapping("/info")
	public String info() {
		LOG.info("Quickstart [{}] Service [{}:{}] is called. datasource = [{}].", appName, ip, port, dataSourceProperties);
		return String.format("Quickstart [%s] Service [%s:%s] is called. datasource = [%s].", appName, ip, port, dataSourceProperties);
	}

	/**
	 * Get metadata in HTTP header.
	 *
	 * @param metadataStr metadata string
	 * @return metadata in HTTP header
	 * @throws UnsupportedEncodingException encoding exception
	 */
	@RequestMapping("/echo")
	public String echoHeader(@RequestHeader(MetadataConstant.HeaderName.CUSTOM_METADATA) String metadataStr)
			throws UnsupportedEncodingException {
		LOG.info(URLDecoder.decode(metadataStr, UTF_8));
		metadataStr = URLDecoder.decode(metadataStr, UTF_8);
		return metadataStr;
	}

	/**
	 * Check circuit break.
	 *
	 * @return circuit break info
	 */
	@GetMapping("/circuitBreak")
	public String circuitBreak() {
		LOG.info("Quickstart Callee Service [{}:{}] is called right.", ip, port);
		return String.format("Quickstart Callee Service [%s:%s] is called right.", ip, port);
	}

	@GetMapping("/faultDetect")
	public String health() {
		LOG.info("Quickstart Callee Service [{}:{}] is detected right.", ip, port);
		return String.format("Quickstart Callee Service [%s:%s] is detected right.", ip, port);
	}
}
