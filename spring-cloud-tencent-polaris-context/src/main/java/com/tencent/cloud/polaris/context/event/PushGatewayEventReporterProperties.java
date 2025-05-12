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

package com.tencent.cloud.polaris.context.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Polaris push gateway event reporter.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.event.pushgateway")
public class PushGatewayEventReporterProperties {

	/**
	 * If push gateway event enabled.
	 */
	private boolean enabled = false;

	/**
	 * Address of pushgateway. For example: 1.2.3.4:9091.
	 */
	private String address;

	/**
	 * Queue size for push gateway event queue. Default is 1000.
	 */
	private int eventQueueSize = 1000;

	/**
	 * Max batch size for push gateway event. Default is 100.
	 */
	private int maxBatchSize = 100;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getEventQueueSize() {
		return eventQueueSize;
	}

	public void setEventQueueSize(int eventQueueSize) {
		this.eventQueueSize = eventQueueSize;
	}

	public int getMaxBatchSize() {
		return maxBatchSize;
	}

	public void setMaxBatchSize(int maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}

	@Override
	public String toString() {
		return "PushGatewayEventReporterProperties{" +
				"enabled=" + enabled +
				", address='" + address + '\'' +
				", eventQueueSize=" + eventQueueSize +
				", maxBatchSize=" + maxBatchSize +
				'}';
	}
}
