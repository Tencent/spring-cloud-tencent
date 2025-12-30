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

package com.tencent.cloud.plugin.mq.lane.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.polaris.lane.kafka")
public class KafkaLaneProperties {

	/**
	 * enable kafka lane.
	 */
	private Boolean laneOn = false;

	/**
	 * lane listener whether to consume main message.
	 * scene: message without lane label, listener service with lane label, force lane service to consume main message.
	 * default false.
	 */
	private Boolean laneConsumeMain = false;

	/**
	 * main listener whether to consume lane message.
	 * scene: message with lane label, lane not deployed or not online, main consume lane message.
	 * default false.
	 */
	private Boolean mainConsumeLane = false;

	/**
	 * whether to set lane id to message header.
	 */
	private Boolean autoSetHeader = true;

	public Boolean getLaneOn() {
		return laneOn;
	}

	public void setLaneOn(Boolean laneOn) {
		this.laneOn = laneOn;
	}

	public Boolean getLaneConsumeMain() {
		return laneConsumeMain;
	}

	public void setLaneConsumeMain(Boolean laneConsumeMain) {
		this.laneConsumeMain = laneConsumeMain;
	}

	public Boolean getMainConsumeLane() {
		return mainConsumeLane;
	}

	public void setMainConsumeLane(Boolean mainConsumeLane) {
		this.mainConsumeLane = mainConsumeLane;
	}

	public Boolean getAutoSetHeader() {
		return autoSetHeader;
	}

	public void setAutoSetHeader(Boolean autoSetHeader) {
		this.autoSetHeader = autoSetHeader;
	}
}
