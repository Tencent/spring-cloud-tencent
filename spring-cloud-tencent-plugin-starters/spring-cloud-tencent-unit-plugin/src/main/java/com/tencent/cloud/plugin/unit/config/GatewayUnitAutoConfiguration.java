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

package com.tencent.cloud.plugin.unit.config;

import javax.annotation.PostConstruct;

import com.tencent.cloud.common.tsf.ConditionalOnOnlyTsfConsulEnabled;
import com.tencent.tsf.unit.core.GatewayUnitArchCallback;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.TsfZoneFilterUnitCallback;
import com.tencent.tsf.unit.core.remote.TsfUnitConsulManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GatewayUnitAutoConfiguration {

	/**
	 * 网关时执行.
	 */
	@Configuration
	@ConditionalOnOnlyTsfConsulEnabled
	@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
	static class GatewayUnitEnable {

		@Value("${spring.application.name:}")
		private String applicationName;


		@PostConstruct
		public void init() {
			TencentUnitManager.addArchCallback(new GatewayUnitArchCallback(applicationName));
			TencentUnitManager.addRuleCallback(new TsfZoneFilterUnitCallback());
			TsfUnitConsulManager.init();
		}
	}
}
