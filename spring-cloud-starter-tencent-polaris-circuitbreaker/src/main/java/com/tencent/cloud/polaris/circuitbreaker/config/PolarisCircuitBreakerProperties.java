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

package com.tencent.cloud.polaris.circuitbreaker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties of Polaris CircuitBreaker .
 *
 */
@ConfigurationProperties("spring.cloud.polaris.circuitbreaker")
public class PolarisCircuitBreakerProperties {

	/**
	 * Whether enable polaris circuit-breaker function.
	 */
	@Value("${spring.cloud.polaris.circuitbreaker.enabled:#{true}}")
	private boolean enabled = true;

	/**
	 * Interval to clean up PolarisCircuitBreakerConfiguration, unit millisecond.
	 */
	@Value("${spring.cloud.polaris.circuitbreaker.configuration-cleanup-interval:#{300000}}")
	private long configurationCleanupInterval = 300000;

	/**
	 * If default circuit breaker rule is enabled. Default is true.
	 */
	private boolean defaultRuleEnabled = true;

	/**
	 * Consecutive error count circuit-breaker default error count. Default is 10.
	 */
	private int defaultErrorCount = 10;

	/**
	 * Error rate circuit-breaker default error rate percent. Default is 50.
	 */
	private int defaultErrorPercent = 50;

	/**
	 * Error rate circuit-breaker default interval(ms). Default is 60000.
	 */
	private int defaultInterval = 60000;

	/**
	 * Error rate circuit-breaker default minimum request. Default is 10.
	 */
	private int defaultMinimumRequest = 10;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getConfigurationCleanupInterval() {
		return configurationCleanupInterval;
	}

	public void setConfigurationCleanupInterval(long configurationCleanupInterval) {
		this.configurationCleanupInterval = configurationCleanupInterval;
	}

	public boolean isDefaultRuleEnabled() {
		return defaultRuleEnabled;
	}

	public void setDefaultRuleEnabled(boolean defaultRuleEnabled) {
		this.defaultRuleEnabled = defaultRuleEnabled;
	}

	public int getDefaultErrorCount() {
		return defaultErrorCount;
	}

	public void setDefaultErrorCount(int defaultErrorCount) {
		this.defaultErrorCount = defaultErrorCount;
	}

	public int getDefaultErrorPercent() {
		return defaultErrorPercent;
	}

	public void setDefaultErrorPercent(int defaultErrorPercent) {
		this.defaultErrorPercent = defaultErrorPercent;
	}

	public int getDefaultInterval() {
		return defaultInterval;
	}

	public void setDefaultInterval(int defaultInterval) {
		this.defaultInterval = defaultInterval;
	}

	public int getDefaultMinimumRequest() {
		return defaultMinimumRequest;
	}

	public void setDefaultMinimumRequest(int defaultMinimumRequest) {
		this.defaultMinimumRequest = defaultMinimumRequest;
	}

	@Override
	public String toString() {
		return "PolarisCircuitBreakerProperties{" +
				"enabled=" + enabled +
				", configurationCleanupInterval=" + configurationCleanupInterval +
				", defaultRuleEnabled=" + defaultRuleEnabled +
				", defaultErrorCount=" + defaultErrorCount +
				", defaultErrorPercent=" + defaultErrorPercent +
				", defaultInterval=" + defaultInterval +
				", defaultMinimumRequest=" + defaultMinimumRequest +
				'}';
	}
}
