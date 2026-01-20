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
package com.tencent.cloud.polaris.context.logging;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggingConsts;
import com.tencent.polaris.logging.PolarisLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.NonNull;

/**
 * Reload of Polaris logging configuration.
 *
 * @author Haotian Zhang
 */
public class PolarisLoggingApplicationListener implements GenericApplicationListener {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisLoggingApplicationListener.class);

	private static final int ORDER = LoggingApplicationListener.DEFAULT_ORDER + 2;

	@Override
	public boolean supportsEventType(ResolvableType resolvableType) {
		Class<?> type = resolvableType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(type)
				|| ApplicationFailedEvent.class.isAssignableFrom(type)
				|| EnvironmentChangeEvent.class.isAssignableFrom(type)
				|| WebServerInitializedEvent.class.isAssignableFrom(type);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationEvent applicationEvent) {
		ConfigurableEnvironment environment = null;

		if (ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(applicationEvent.getClass())) {
			environment = ((ApplicationEnvironmentPreparedEvent) applicationEvent).getEnvironment();
		}
		else if (ApplicationFailedEvent.class.isAssignableFrom(applicationEvent.getClass())) {
			ConfigurableApplicationContext configurableApplicationContext = ((ApplicationFailedEvent) applicationEvent)
					.getApplicationContext();
			if (configurableApplicationContext != null) {
				environment = configurableApplicationContext.getEnvironment();
			}
		}

		if (environment != null) {
			String loggingPath = environment.getProperty("spring.cloud.polaris.logging.path");
			if (StringUtils.isNotBlank(loggingPath)) {
				System.setProperty(LoggingConsts.LOGGING_PATH_PROPERTY, loggingPath);
			}
		}
		LOG.info("Polaris logging configuration reloaded in {}.", applicationEvent.getClass().getSimpleName());
		PolarisLogging.getInstance().loadConfiguration();
	}
}
