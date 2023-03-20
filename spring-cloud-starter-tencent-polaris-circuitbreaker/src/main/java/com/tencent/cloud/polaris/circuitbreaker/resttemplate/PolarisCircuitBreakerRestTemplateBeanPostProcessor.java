package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

public class PolarisCircuitBreakerRestTemplateBeanPostProcessor implements MergedBeanDefinitionPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(PolarisCircuitBreakerRestTemplateBeanPostProcessor.class);
	private final ApplicationContext applicationContext;

	public PolarisCircuitBreakerRestTemplateBeanPostProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	private final ConcurrentHashMap<String, PolarisCircuitBreakerRestTemplate> cache = new ConcurrentHashMap<>();

	private void checkPolarisCircuitBreakerRestTemplate(PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate) {
		if (
				StringUtils.hasText(polarisCircuitBreakerRestTemplate.fallback()) &&
						!PolarisCircuitBreakerFallback.class.toGenericString().equals(polarisCircuitBreakerRestTemplate.fallbackClass().toGenericString())
		) {
			throw new IllegalArgumentException("PolarisCircuitBreakerRestTemplate's fallback and fallbackClass could not set at sametime !");
		}
	}

	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		if (checkAnnotated(beanDefinition, beanType, beanName)) {
			PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate;
			if (beanDefinition.getSource() instanceof StandardMethodMetadata) {
				polarisCircuitBreakerRestTemplate = ((StandardMethodMetadata) beanDefinition.getSource()).getIntrospectedMethod()
						.getAnnotation(PolarisCircuitBreakerRestTemplate.class);
			}
			else {
				polarisCircuitBreakerRestTemplate = beanDefinition.getResolvedFactoryMethod()
						.getAnnotation(PolarisCircuitBreakerRestTemplate.class);
			}
			checkPolarisCircuitBreakerRestTemplate(polarisCircuitBreakerRestTemplate);
			cache.put(beanName, polarisCircuitBreakerRestTemplate);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (cache.containsKey(beanName)) {
			// add interceptor for each RestTemplate with @PolarisCircuitBreakerRestTemplate annotation
			StringBuilder interceptorBeanNamePrefix = new StringBuilder();
			PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate = cache.get(beanName);
			interceptorBeanNamePrefix
					.append(StringUtils.uncapitalize(
							PolarisCircuitBreakerRestTemplate.class.getSimpleName()))
					.append("_")
					.append(polarisCircuitBreakerRestTemplate.fallbackClass().getSimpleName());
			RestTemplate restTemplate = (RestTemplate) bean;
			String interceptorBeanName = interceptorBeanNamePrefix + "@" + bean;
			CircuitBreakerFactory circuitBreakerFactory = this.applicationContext.getBean(CircuitBreakerFactory.class);
			registerBean(interceptorBeanName, polarisCircuitBreakerRestTemplate, applicationContext, circuitBreakerFactory, restTemplate);
			PolarisCircuitBreakerRestTemplateInterceptor polarisCircuitBreakerRestTemplateInterceptor = applicationContext
					.getBean(interceptorBeanName, PolarisCircuitBreakerRestTemplateInterceptor.class);
			restTemplate.getInterceptors().add(0, polarisCircuitBreakerRestTemplateInterceptor);
		}
		return bean;
	}

	private boolean checkAnnotated(RootBeanDefinition beanDefinition,
			Class<?> beanType, String beanName) {
		return beanName != null && beanType == RestTemplate.class
				&& beanDefinition.getSource() instanceof MethodMetadata
				&& ((MethodMetadata) beanDefinition.getSource())
				.isAnnotated(PolarisCircuitBreakerRestTemplate.class.getName());
	}

	private void registerBean(String interceptorBeanName, PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate,
			ApplicationContext applicationContext, CircuitBreakerFactory circuitBreakerFactory, RestTemplate restTemplate) {
		// register PolarisCircuitBreakerRestTemplateInterceptor bean
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
				.getAutowireCapableBeanFactory();
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(PolarisCircuitBreakerRestTemplateInterceptor.class);
		beanDefinitionBuilder.addConstructorArgValue(polarisCircuitBreakerRestTemplate);
		beanDefinitionBuilder.addConstructorArgValue(applicationContext);
		beanDefinitionBuilder.addConstructorArgValue(circuitBreakerFactory);
		beanDefinitionBuilder.addConstructorArgValue(restTemplate);
		BeanDefinition interceptorBeanDefinition = beanDefinitionBuilder
				.getRawBeanDefinition();
		beanFactory.registerBeanDefinition(interceptorBeanName,
				interceptorBeanDefinition);
	}

}
