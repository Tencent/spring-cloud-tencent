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

package com.tencent.cloud.plugin.trafficmirroring.config;

import com.tencent.cloud.plugin.trafficmirroring.TrafficMirroringExceptionPlugin;
import com.tencent.cloud.plugin.trafficmirroring.TrafficMirroringPostPlugin;
import com.tencent.cloud.polaris.router.config.ConditionalOnPolarisRouterEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for traffic mirroring plugin.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@ConditionalOnProperty("spring.cloud.polaris.traffic-mirroring.enabled")
@EnableConfigurationProperties(TrafficMirroringProperties.class)
public class TrafficMirroringAutoConfiguration {

	@Bean
	public TrafficMirroringPostPlugin trafficMirrorPostPlugin(TrafficMirroringProperties trafficMirrorProperties) {
		return new TrafficMirroringPostPlugin(trafficMirrorProperties);
	}

	@Bean
	public TrafficMirroringExceptionPlugin trafficMirrorExceptionPlugin(TrafficMirroringProperties trafficMirrorProperties) {
		return new TrafficMirroringExceptionPlugin(trafficMirrorProperties);
	}
}
