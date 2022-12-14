/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Prompt user if web dependence was not imported.
 *
 * @author Daifu Wu
 */
public class PolarisWebApplicationCheck implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisWebApplicationCheck.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		SpringApplication springApplication = event.getSpringApplication();
		WebApplicationType webApplicationType = springApplication.getWebApplicationType();
		if (webApplicationType.equals(WebApplicationType.NONE)) {
			LOGGER.warn("This service instance will not be registered, because it is not a servlet-based or reactive web application.");
		}
	}
}
