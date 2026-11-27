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

import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Example callback that records the instance going offline.
 *
 * @author evelynwei
 */
@Configuration(proxyBeanMethods = false)
public class OfflineCallbackConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineCallbackConfiguration.class);

	@Bean
	@Order(100)
	public LosslessOfflineCallback offlineRecordCallback() {
		return context -> LOGGER.info(
				"Lossless offline callback received for service {}/{}, instance {}:{}, deadline {}.",
				context.getNamespace(), context.getService(), context.getHost(), context.getPort(),
				context.getDeadline());
	}
}
