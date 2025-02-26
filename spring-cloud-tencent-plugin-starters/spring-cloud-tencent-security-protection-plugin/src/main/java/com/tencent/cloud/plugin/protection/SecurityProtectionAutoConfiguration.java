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

package com.tencent.cloud.plugin.protection;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;

/**
 * SecurityProtectionAutoConfiguration.
 *
 * @author Shedfree Wu
 */
@Configuration
public class SecurityProtectionAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProtectionAutoConfiguration.class);

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.tencent.security.protection.servlet.enabled", matchIfMissing = true)
	@ConditionalOnClass(name = {"org.springframework.web.servlet.function.RouterFunction"})
	static class ServletProtectionConfiguration implements InitializingBean {

		@Autowired(required = false)
		List<RouterFunction> routerFunctions;

		@Autowired
		ApplicationContext applicationContext;

		@Override
		public void afterPropertiesSet() {
			if (routerFunctions != null && !routerFunctions.isEmpty()) {
				LOGGER.error("Detected the presence of webmvc RouterFunction-related beans, which may trigger the CVE-2024-38819 vulnerability. The program will soon exit.");
				LOGGER.error("routerFunctions:{}: ", routerFunctions);

				ExitUtils.exit(applicationContext);
			}
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.tencent.security.protection.reactive.enabled", matchIfMissing = true)
	@ConditionalOnClass(name = {"org.springframework.web.reactive.function.server.RouterFunction"})
	static class ReactiveProtectionConfiguration implements InitializingBean {

		@Autowired(required = false)
		List<org.springframework.web.reactive.function.server.RouterFunction> routerFunctions;

		@Autowired
		ApplicationContext applicationContext;

		@Override
		public void afterPropertiesSet() {
			if (routerFunctions != null && !routerFunctions.isEmpty()) {
				LOGGER.error("Detected the presence of webflux RouterFunction-related beans, which may trigger the CVE-2024-38819 vulnerability. The program will soon exit.");
				LOGGER.error("routerFunctions:{}: ", routerFunctions);
				ExitUtils.exit(applicationContext);
			}
		}
	}

}
