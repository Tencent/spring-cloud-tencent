package com.tencent.cloud.polaris.circuitbreaker;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * @author : wh
 * @date : 2022/6/21 21:20
 * @description:
 */
public class PolarisRestTemplateBeanPostProcessor implements ApplicationContextAware, SmartInitializingSingleton {

	private ApplicationContext applicationContext;

	private final PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler;

	public PolarisRestTemplateBeanPostProcessor(PolarisRestTemplateResponseErrorHandler polarisRestTemplateResponseErrorHandler) {
		this.polarisRestTemplateResponseErrorHandler = polarisRestTemplateResponseErrorHandler;
	}

	@Override
	public void afterSingletonsInstantiated() {
		RestTemplate restTemplate = this.applicationContext.getBean(RestTemplate.class);
		if (Objects.nonNull(restTemplate)) {
			restTemplate.setErrorHandler(polarisRestTemplateResponseErrorHandler);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;

	}
}
