package com.tencent.cloud.polaris.circuitbreaker.config;

import com.tencent.cloud.polaris.circuitbreaker.feign.PolarisCircuitBreakerNameResolver;
import feign.Feign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Feign.class, FeignClientFactoryBean.class })
@ConditionalOnPolarisCircuitBreakerEnabled
public class PolarisCircuitBreakerFeignClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(CircuitBreakerNameResolver.class)
	public CircuitBreakerNameResolver polarisCircuitBreakerNameResolver() {
		return new PolarisCircuitBreakerNameResolver();
	}

}
