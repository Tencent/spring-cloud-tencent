package com.tencent.cloud.polaris.circuitbreaker.config;

import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreakerRestTemplateBeanPostProcessor;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisCircuitBreakerEnabled
@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
@AutoConfigureAfter(PolarisCircuitBreakerAutoConfiguration.class)
public class PolarisCircuitBreakerRestTemplateAutoConfiguration {

	@Bean
	public static PolarisCircuitBreakerRestTemplateBeanPostProcessor polarisCircuitBreakerRestTemplateBeanPostProcessor(
			ApplicationContext applicationContext) {
		return new PolarisCircuitBreakerRestTemplateBeanPostProcessor(applicationContext);
	}

}
