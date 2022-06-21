package com.tencent.cloud.polaris.circuitbreaker;

import com.tencent.cloud.polaris.context.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.core.ConsumerAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author : wh
 * @date : 2022/6/21 21:34
 * @description:
 */
@ConditionalOnProperty(value = "spring.cloud.polaris.circuitbreaker.enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(PolarisContextAutoConfiguration.class)
public class PolarisRestTemplateAutoConfiguration {

	@Bean
	@ConditionalOnBean(RestTemplate.class)
	public PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler(ConsumerAPI consumerAPI, @Autowired(required = false) PolarisResponseErrorHandler polarisResponseErrorHandler) {
		return new PolarisRestTemplateResponseErrorHandler(consumerAPI, polarisResponseErrorHandler);
	}

	@Bean
	@ConditionalOnBean(RestTemplate.class)
	public PolarisRestTemplateBeanPostProcessor polarisRestTemplateBeanPostProcessor(PolarisRestTemplateResponseErrorHandler restTemplateResponseErrorHandler) {
		return new PolarisRestTemplateBeanPostProcessor(restTemplateResponseErrorHandler);
	}
}
