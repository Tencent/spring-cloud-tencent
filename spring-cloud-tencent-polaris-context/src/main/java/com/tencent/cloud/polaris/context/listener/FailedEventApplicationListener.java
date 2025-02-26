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

package com.tencent.cloud.polaris.context.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Failed event listener.
 *
 * @author skyehtzhang
 */
@Configuration
public class FailedEventApplicationListener implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(FailedEventApplicationListener.class);

	private ApplicationContext applicationContext;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationFailedEvent) {
			ApplicationFailedEvent failedEvent = (ApplicationFailedEvent) event;
			if (failedEvent.getException() != null) {
				logger.error("[onApplicationEvent] exception in failed event", failedEvent.getException());
			}

			if (applicationContext instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) applicationContext).close();
			}
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException e) {
				// do nothing
			}
			System.exit(0);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
