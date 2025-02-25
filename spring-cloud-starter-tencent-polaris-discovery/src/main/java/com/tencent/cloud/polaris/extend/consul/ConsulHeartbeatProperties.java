/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.extend.consul;

import java.time.Duration;

import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;

/**
 * Copy from org.springframework.cloud.consul.discovery.HeartbeatProperties.
 * Properties related to heartbeat verification.
 *
 * @author Spencer Gibb
 * @author Chris Bono
 */
@ConfigurationProperties(prefix = "spring.cloud.consul.discovery.heartbeat")
public class ConsulHeartbeatProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsulHeartbeatProperties.class);
	// TODO: change enabled to default to true when I stop seeing messages like
	// [WARN] agent: Check 'service:testConsulApp:xtest:8080' missed TTL, is now critical
	boolean enabled = true;

	private int ttlValue = 30;

	private String ttlUnit = "s";

	private double intervalRatio = 2.0 / 3.0;

	//TODO: did heartbeatInterval need to be a field?

	protected Duration computeHeartbeatInterval() {
		// heartbeat rate at ratio * ttl, but no later than ttl -1s and, (under lesser
		// priority), no sooner than 1s from now
		double interval = ttlValue * intervalRatio;
		double max = Math.max(interval, 1);
		int ttlMinus1 = ttlValue - 1;
		double min = Math.min(ttlMinus1, max);
		Duration heartbeatInterval = Duration.ofMillis(Math.round(1000 * min));
		LOGGER.debug("Computed heartbeatInterval: " + heartbeatInterval);
		return heartbeatInterval;
	}

	public String getTtl() {
		return ttlValue + ttlUnit;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getTtlValue() {
		return this.ttlValue;
	}

	public void setTtlValue(int ttlValue) {
		if (ttlValue < 1) {
			LOGGER.error("ttlValue must be at least 1, invalid value: {}", ttlValue);
			throw new IllegalArgumentException("ttlValue must be at least 1");
		}
		this.ttlValue = ttlValue;
	}

	public String getTtlUnit() {
		return this.ttlUnit;
	}

	public void setTtlUnit(String ttlUnit) {
		if (StringUtils.isEmpty(ttlUnit)) {
			LOGGER.error("ttlUnit cannot be null or empty");
			throw new IllegalArgumentException("ttlUnit cannot be null or empty");
		}
		this.ttlUnit = ttlUnit;
	}

	public double getIntervalRatio() {
		return this.intervalRatio;
	}

	public void setIntervalRatio(double intervalRatio) {
		if (intervalRatio < 0.1 || intervalRatio > 0.9) {
			LOGGER.error("intervalRatio must be between 0.1 and 0.9, invalid value: {}", intervalRatio);
			throw new IllegalArgumentException("intervalRatio must be between 0.1 and 0.9");
		}
		this.intervalRatio = intervalRatio;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("enabled", enabled)
				.append("ttlValue", ttlValue)
				.append("ttlUnit", ttlUnit)
				.append("intervalRatio", intervalRatio)
				.toString();
	}
}
