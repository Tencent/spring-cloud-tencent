/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */
package com.tencent.cloud.polaris.config.logger;

import com.tencent.polaris.logging.PolarisLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author juanyinyang
 */
public class PolarisConfigLoggerApplicationListener implements ApplicationListener<ApplicationEvent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigLoggerApplicationListener.class);
	/**
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		try {
			// Initialize application loggingSystem.
			if (event instanceof ApplicationStartedEvent) {
				ApplicationStartedEvent startedEvent = (ApplicationStartedEvent) event;
				ClassLoader classLoader = startedEvent.getSpringApplication().getClassLoader();
				LoggingSystem loggingSystem = LoggingSystem.get(classLoader);
				LOGGER.info("PolarisConfigLoggerApplicationListener onApplicationEvent init loggingSystem:{}", loggingSystem);
				PolarisConfigLoggerContext.setLogSystem(loggingSystem);
				PolarisLogging.getInstance().loadConfiguration();
			}
		}
		catch (Exception e) {
			LOGGER.error("PolarisConfigLoggerApplicationListener onApplicationEvent exception:", e);
		}
	}
}
