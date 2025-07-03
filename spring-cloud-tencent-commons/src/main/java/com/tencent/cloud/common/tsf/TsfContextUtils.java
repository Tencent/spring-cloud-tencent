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

package com.tencent.cloud.common.tsf;

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

	private static final AtomicBoolean isTsfConsulEnabledFirstConfiguration = new AtomicBoolean(true);

	private static final AtomicBoolean isOnlyTsfConsulEnabledFirstConfiguration = new AtomicBoolean(true);

	private static boolean tsfConsulEnabled = false;

	private static boolean onlyTsfConsulEnabled = false;

	private TsfContextUtils() {
	}

	public static boolean isTsfConsulEnabled(Environment environment) {
		if (environment != null && isTsfConsulEnabledFirstConfiguration.compareAndSet(true, false)) {
			if (isOnlyTsfConsulEnabled(environment)) {
				tsfConsulEnabled = true;
			}
			else {
				boolean consulEnabled = Boolean.parseBoolean(environment.getProperty("tsf_consul_enable", "true"));
				String tsfConsulIp = environment.getProperty("tsf_consul_ip");
				tsfConsulEnabled = consulEnabled && StringUtils.isNotBlank(tsfConsulIp);
				if (tsfConsulEnabled) {
					LOG.info("Tsf Consul is enabled: {}", tsfConsulIp);
				}
			}
		}
		return tsfConsulEnabled;
	}

	public static boolean isOnlyTsfConsulEnabled(Environment environment) {
		if (environment != null && isOnlyTsfConsulEnabledFirstConfiguration.compareAndSet(true, false)) {
			boolean consulEnabled = Boolean.parseBoolean(environment.getProperty("tsf_consul_enable", "true"));
			String tsfConsulIp = environment.getProperty("tsf_consul_ip");
			String polarisAddress = environment.getProperty("polaris_address");
			if (StringUtils.isBlank(polarisAddress) && StringUtils.isNotBlank(environment.getProperty("spring.cloud.polaris.address"))) {
				polarisAddress = environment.getProperty("spring.cloud.polaris.address");
			}
			onlyTsfConsulEnabled = consulEnabled && StringUtils.isNotBlank(tsfConsulIp) && StringUtils.isBlank(polarisAddress);
			if (onlyTsfConsulEnabled) {
				LOG.info("Only Tsf Consul is enabled: {}", tsfConsulIp);
			}
		}
		return onlyTsfConsulEnabled;
	}

	/**
	 * This method should be called after {@link com.tencent.cloud.common.tsf.TsfContextUtils#isOnlyTsfConsulEnabled(Environment)}.
	 * @return whether only Tsf Consul is enabled
	 */
	public static boolean isOnlyTsfConsulEnabled() {
		return onlyTsfConsulEnabled;
	}
}
