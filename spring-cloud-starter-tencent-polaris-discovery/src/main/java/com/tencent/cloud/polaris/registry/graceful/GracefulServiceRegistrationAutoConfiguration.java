package com.tencent.cloud.polaris.registry.graceful;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Spencer Gibb
 */
@Configuration(proxyBeanMethods = false)
@Import(GracefulServiceRegistrationConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.service-registry.graceful-registration.enabled", matchIfMissing = true)
public class GracefulServiceRegistrationAutoConfiguration {

	@Autowired(required = false)
	private GracefulServiceRegistration gracefulServiceRegistration;

	@Autowired
	private GracefulServiceRegistrationProperties properties;

	@PostConstruct
	protected void init() {
		if (this.gracefulServiceRegistration == null && this.properties.isFailFast()) {
			throw new IllegalStateException("Graceful Service Registration has "
					+ "been requested, but there is no GracefulServiceRegistration bean");
		}
	}

}
