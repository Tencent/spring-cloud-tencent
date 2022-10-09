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

import com.tencent.cloud.common.constant.MetadataConstant;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Gateway callee controller.
 *
 * @author Haotian Zhang
 */
@RestController
@RequestMapping("/gateway/example/callee")
public class GatewayCalleeController {

	private static Logger LOG = LoggerFactory.getLogger(GatewayCalleeController.class);

	@Value("${server.port:0}")
	private int port;

	/**
	 * Get information of callee.
	 * @return information of callee
	 */
	@RequestMapping("/info")
	public String info() {
		LOG.info("Gateway Example Callee [{}] is called.", port);
		return String.format("Gateway Example Callee [%s] is called.", port);
	}

	/**
	 * Get metadata in HTTP header.
	 *
	 * @param metadataStr metadata string
	 * @return metadata in HTTP header
	 * @throws UnsupportedEncodingException encoding exception
	 */
	@RequestMapping("/echo")
	public String echoHeader(
			@RequestHeader(MetadataConstant.HeaderName.CUSTOM_METADATA) String metadataStr)
			throws UnsupportedEncodingException {
		LOG.info(URLDecoder.decode(metadataStr, UTF_8));
		metadataStr = URLDecoder.decode(metadataStr, UTF_8);
		return cleanXSS(metadataStr);
	}

	private String cleanXSS(String str) {
		str = ESAPI.encoder().encodeForHTML(str);
		return str;
	}
}
