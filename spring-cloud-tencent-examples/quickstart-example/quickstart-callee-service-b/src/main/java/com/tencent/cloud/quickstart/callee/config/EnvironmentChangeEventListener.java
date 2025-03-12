package com.tencent.cloud.quickstart.callee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentChangeEventListener {
	private static final Logger LOG = LoggerFactory.getLogger(EnvironmentChangeEventListener.class);

	private final Environment environment;

	public EnvironmentChangeEventListener(Environment environment) {
		this.environment = environment;
	}

	@EventListener
	public void handleConfigChange(EnvironmentChangeEvent event) {
		if (event == null) {
			LOG.warn("Received null environment change event");
			return;
		}

		if (event.getKeys().isEmpty()) {
			LOG.warn("Received empty keys in environment change event. Event details: {}", event);
			LOG.info("Current environment properties, Active: {}, Default:{}", environment.getActiveProfiles(),
					environment.getDefaultProfiles());
			return;
		}

		StringBuilder changes = new StringBuilder();
		changes.append("Environment configuration changes:\n");

		event.getKeys().forEach(key -> {
			String value = environment.getProperty(key);
			changes.append(String.format("  %s = %s%n", key, value));
		});

		LOG.info(changes.toString());
	}
}

