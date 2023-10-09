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
 *
 */

package com.tencent.cloud.polaris.config.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Maps;
import com.tencent.cloud.polaris.config.spring.event.ConfigChangeSpringEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.NonNull;

import static com.tencent.cloud.polaris.config.listener.PolarisConfigListenerContext.fireConfigChange;
import static com.tencent.cloud.polaris.config.listener.PolarisConfigListenerContext.initialize;
import static com.tencent.cloud.polaris.config.listener.PolarisConfigListenerContext.merge;

/**
 * Polaris Config Change Event Listener .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022-06-08
 */
public final class PolarisConfigChangeEventListener implements ApplicationListener<ApplicationEvent>, ApplicationEventPublisherAware {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisConfigChangeEventListener.class);

	private static final AtomicBoolean started = new AtomicBoolean();

	private ApplicationEventPublisher eventPublisher;

	/**
	 * Handle an application event.
	 *
	 * @param event the event to respond to
	 */
	@Override
	public void onApplicationEvent(@NonNull ApplicationEvent event) {
		// Initialize application all environment properties .
		if (event instanceof ApplicationStartedEvent && started.compareAndSet(false, true)) {
			ApplicationStartedEvent applicationStartedEvent = (ApplicationStartedEvent) event;
			ConfigurableEnvironment environment = applicationStartedEvent.getApplicationContext().getEnvironment();
			Map<String, Object> ret = loadEnvironmentProperties(environment);
			if (!ret.isEmpty()) {
				initialize(ret);
			}
		}

		// Process Environment Change Event .
		if (event instanceof EnvironmentChangeEvent) {
			EnvironmentChangeEvent environmentChangeEvent = (EnvironmentChangeEvent) event;
			ConfigurableApplicationContext context = (ConfigurableApplicationContext) environmentChangeEvent.getSource();
			ConfigurableEnvironment environment = context.getEnvironment();
			Map<String, Object> ret = loadEnvironmentProperties(environment);
			Map<String, ConfigPropertyChangeInfo> changes = merge(ret);
			ConfigChangeSpringEvent configChangeSpringEvent = new ConfigChangeSpringEvent(new HashMap<>(changes));
			eventPublisher.publishEvent(configChangeSpringEvent);
			fireConfigChange(changes.keySet(), new HashMap<>(changes));
			changes.clear();
		}
	}

	/**
	 * Try load all application environment config properties .
	 * @param environment application environment instance of {@link Environment}
	 * @return properties
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadEnvironmentProperties(ConfigurableEnvironment environment) {
		Map<String, Object> ret = Maps.newHashMap();
		MutablePropertySources sources = environment.getPropertySources();
		sources.iterator().forEachRemaining(propertySource -> {
			// Don't read system env variable.
			if (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(propertySource.getName())) {
				return;
			}

			Object o = propertySource.getSource();
			if (o instanceof Map) {
				for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
					String key = entry.getKey();
					try {
						String value = environment.getProperty(key);
						ret.put(key, value);
					}
					catch (Exception e) {
						LOG.warn("Read property from {} with key {} failed.", propertySource.getName(), key, e);
					}

				}
			}
			else if (o instanceof Collection) {
				int count = 0;
				Collection<Object> collection = (Collection<Object>) o;
				for (Object object : collection) {
					String key = "[" + (count++) + "]";
					ret.put(key, object);
				}
			}
		});
		return ret;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}
}
