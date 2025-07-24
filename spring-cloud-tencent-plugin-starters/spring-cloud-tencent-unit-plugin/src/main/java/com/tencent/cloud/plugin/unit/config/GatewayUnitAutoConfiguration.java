/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.cloud.plugin.unit.config;

import com.tencent.cloud.common.tsf.ConditionalOnOnlyTsfConsulEnabled;
import com.tencent.tsf.unit.core.GatewayUnitArchCallback;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.TsfZoneFilterUnitCallback;
import com.tencent.tsf.unit.core.remote.TsfUnitConsulManager;
import jakarta.annotation.PostConstruct;

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
