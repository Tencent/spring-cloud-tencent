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

package com.tencent.cloud.polaris.gateway.example.callee;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.tencent.cloud.metadata.constant.MetadataConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/gateway/example/callee")
public class GatewayCalleeController {

	private static Logger logger = LoggerFactory.getLogger(GatewayCalleeController.class);

	/**
	 * Get info string
	 * @return 返回服务信息
	 */
	@RequestMapping("/info")
	public String info() {
		return "Gateway Example Callee";
	}

	/**
	 * Get metadata in HTTP header
	 */
	@RequestMapping("/echo")
	public String echoHeader(
			@RequestHeader(MetadataConstant.HeaderName.CUSTOM_METADATA) String metadataStr)
			throws UnsupportedEncodingException {
		logger.info(URLDecoder.decode(metadataStr, "UTF-8"));
		return URLDecoder.decode(metadataStr, "UTF-8");
	}

}
