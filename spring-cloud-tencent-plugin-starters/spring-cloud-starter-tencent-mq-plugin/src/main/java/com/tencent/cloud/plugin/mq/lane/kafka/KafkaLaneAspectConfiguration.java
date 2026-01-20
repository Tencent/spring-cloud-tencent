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

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.plugin.mq.lane.AbstractActiveLane;
import com.tencent.cloud.plugin.mq.lane.PolarisActiveLane;
import com.tencent.cloud.plugin.mq.lane.tsf.TsfActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaLaneProperties.class)
public class KafkaLaneAspectConfiguration {

	@Bean
	@ConditionalOnClass(name = {"org.springframework.kafka.core.KafkaTemplate"})
	public KafkaLaneAspect kafkaLaneAspect(PolarisSDKContextManager polarisSDKContextManager,
			KafkaLaneProperties kafkaLaneProperties,
			AbstractActiveLane activeLane) {
		return new KafkaLaneAspect(polarisSDKContextManager, kafkaLaneProperties, activeLane);
	}

	@Bean
	@ConditionalOnClass(name = {"org.springframework.kafka.core.KafkaTemplate"})
	@ConditionalOnMissingBean
	public AbstractActiveLane activeLane(PolarisSDKContextManager polarisSDKContextManager,
			PolarisDiscoveryHandler discoveryClient, Registration registration) {
		if (TsfContextUtils.isOnlyTsfConsulEnabled()) {
			return new TsfActiveLane(polarisSDKContextManager, discoveryClient);
		}
		else {
			return new PolarisActiveLane(polarisSDKContextManager, discoveryClient, registration);
		}
	}
}
