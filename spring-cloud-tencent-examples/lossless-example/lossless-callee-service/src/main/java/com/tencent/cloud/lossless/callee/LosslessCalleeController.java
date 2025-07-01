/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.lossless.callee;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.common.constant.MetadataConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

@RestController
@RequestMapping("/lossless/callee")
public class LosslessCalleeController {

	private static final Logger LOG = LoggerFactory.getLogger(LosslessCalleeController.class);

	@Value("${lossless.healthy.delay.second:0}")
	private int healthyDelay;

	private final AtomicBoolean calledHealthyEndpoint = new AtomicBoolean(false);

	private final AtomicInteger healthy = new AtomicInteger(0);

	@GetMapping("/health")
	public ResponseEntity<String> health() {
		if (healthy.get() == 1) {
			return new ResponseEntity<>("OK", HttpStatus.OK);
		}
		else {
			if (calledHealthyEndpoint.compareAndSet(false, true)) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						if (healthyDelay > 0) {
							try {
								Thread.sleep(healthyDelay * 1000L);
							}
							catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
						}
						healthy.set(1);
					}
				});
				thread.start();
			}
			return new ResponseEntity<>("NOK", HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	/**
	 * Get metadata in HTTP query.
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

}
