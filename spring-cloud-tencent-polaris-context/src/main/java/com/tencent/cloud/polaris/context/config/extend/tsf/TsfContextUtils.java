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

package com.tencent.cloud.polaris.context.config.extend.tsf;

import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;

/**
 * Utils for TSF.
 *
 * @author Haotian Zhang
 */
public final class TsfContextUtils {

	private static final Logger LOG = LoggerFactory.getLogger(TsfContextUtils.class);

	private static final AtomicBoolean isFirstConfiguration = new AtomicBoolean(true);

	private static boolean tsfConsulEnabled = false;

	private TsfContextUtils() {
	}

	public static boolean isTsfConsulEnabled(Environment environment) {
		if (environment != null && isFirstConfiguration.compareAndSet(true, false)) {
			String tsfConsulIp = environment.getProperty("tsf_consul_ip");
			String tsePolarisAddress = environment.getProperty("polaris_address");
			if (StringUtils.isBlank(tsePolarisAddress) && StringUtils.isNotBlank(environment.getProperty("spring.cloud.polaris.address"))) {
				tsePolarisAddress = environment.getProperty("spring.cloud.polaris.address");
			}
			tsfConsulEnabled = StringUtils.isNotBlank(tsfConsulIp) && StringUtils.isBlank(tsePolarisAddress);
			if (tsfConsulEnabled) {
				LOG.info("Tsf Consul is enabled: {}", tsfConsulIp);
			}
		}
		return tsfConsulEnabled;
	}
}
