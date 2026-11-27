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

package com.tencent.cloud.plugin.lossless.config;

import com.tencent.cloud.plugin.lossless.extension.OfflineCallbackFailureMode;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.polaris.lossless")
public class LosslessProperties {

	private static final long MAX_TOTAL_TIMEOUT_MILLIS = Long.MAX_VALUE / 1_000_000L;

	/**
	 * the switch for lossless plugin.
	 */
	private boolean enabled = false;

	private String healthCheckPath;

	private Long delayRegisterInterval;

	private Long healthCheckInterval;

	/**
	 * Whether offline callbacks are enabled.
	 */
	private boolean offlineCallbackEnabled = true;

	/**
	 * Maximum wait time for one offline callback.
	 */
	private long offlineCallbackTimeout = 10000;

	/**
	 * Maximum wait time for the whole offline callback chain.
	 */
	private long offlineCallbackTotalTimeout = 20000;

	/**
	 * Failure strategy used when an offline callback fails.
	 */
	private OfflineCallbackFailureMode offlineCallbackFailureMode = OfflineCallbackFailureMode.CONTINUE;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHealthCheckPath() {
		return healthCheckPath;
	}

	public void setHealthCheckPath(String healthCheckPath) {
		this.healthCheckPath = healthCheckPath;
	}

	public Long getDelayRegisterInterval() {
		return delayRegisterInterval;
	}

	public void setDelayRegisterInterval(Long delayRegisterInterval) {
		this.delayRegisterInterval = delayRegisterInterval;
	}

	public Long getHealthCheckInterval() {
		return healthCheckInterval;
	}

	public void setHealthCheckInterval(Long healthCheckInterval) {
		this.healthCheckInterval = healthCheckInterval;
	}

	public boolean isOfflineCallbackEnabled() {
		return offlineCallbackEnabled;
	}

	public void setOfflineCallbackEnabled(boolean offlineCallbackEnabled) {
		this.offlineCallbackEnabled = offlineCallbackEnabled;
	}

	public long getOfflineCallbackTimeout() {
		return offlineCallbackTimeout;
	}

	public void setOfflineCallbackTimeout(long offlineCallbackTimeout) {
		if (offlineCallbackTimeout <= 0) {
			throw new IllegalArgumentException("offlineCallbackTimeout must be greater than 0");
		}
		this.offlineCallbackTimeout = offlineCallbackTimeout;
	}

	public long getOfflineCallbackTotalTimeout() {
		return offlineCallbackTotalTimeout;
	}

	public void setOfflineCallbackTotalTimeout(long offlineCallbackTotalTimeout) {
		if (offlineCallbackTotalTimeout <= 0) {
			throw new IllegalArgumentException("offlineCallbackTotalTimeout must be greater than 0");
		}
		if (offlineCallbackTotalTimeout > MAX_TOTAL_TIMEOUT_MILLIS) {
			throw new IllegalArgumentException("offlineCallbackTotalTimeout is too large");
		}
		this.offlineCallbackTotalTimeout = offlineCallbackTotalTimeout;
	}

	public OfflineCallbackFailureMode getOfflineCallbackFailureMode() {
		return offlineCallbackFailureMode;
	}

	public void setOfflineCallbackFailureMode(OfflineCallbackFailureMode offlineCallbackFailureMode) {
		if (offlineCallbackFailureMode == null) {
			throw new IllegalArgumentException("offlineCallbackFailureMode must not be null");
		}
		this.offlineCallbackFailureMode = offlineCallbackFailureMode;
	}
}
