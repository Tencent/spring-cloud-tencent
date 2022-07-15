package com.tencent.cloud.polaris.registry.graceful;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GracefulServiceRegistrationProperties.class)
@ConditionalOnProperty(value = "spring.cloud.service-registry.graceful-registration.enabled")
public class GracefulServiceRegistrationConfiguration {

}
