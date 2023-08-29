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

import java.util.Collections;

import com.tencent.cloud.polaris.config.adapter.PolarisConfigRefreshScopeAnnotationDetector;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import com.tencent.cloud.polaris.config.adapter.PolarisRefreshEntireContextRefresher;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.enums.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.lang.NonNull;

import static com.tencent.cloud.polaris.config.condition.ReflectRefreshTypeCondition.POLARIS_CONFIG_REFRESH_TYPE;

/**
 * When {@link com.tencent.cloud.polaris.config.adapter.PolarisConfigRefreshScopeAnnotationDetector} detects that
 * the annotation {@code @RefreshScope} exists and is used, but the config refresh type
 * {@code spring.cloud.polaris.config.refresh-type} is still {@code RefreshType.REFLECT}, then the framework will
 * automatically switch the config refresh type to {@code RefreshType.REFRESH_CONTEXT}.
 *
 * <p>The purpose of this optimization is to omit additional configuration, and facilitate for users to use the
 * dynamic configuration refresh strategy of Spring Cloud Context.</p>
 *
 * @author jarvisxiong
 */
public class PolarisConfigRefreshOptimizationListener implements ApplicationListener<ContextRefreshedEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigRefreshOptimizationListener.class);

	private static final String CONFIG_REFRESH_TYPE_PROPERTY = "configRefreshTypeProperty";

	private static final String REFLECT_REBINDER_BEAN_NAME = "affectedConfigurationPropertiesRebinder";

	private static final String REFLECT_REFRESHER_BEAN_NAME = "polarisReflectPropertySourceAutoRefresher";

	private static final String REFRESH_CONTEXT_REFRESHER_BEAN_NAME = "polarisRefreshContextPropertySourceAutoRefresher";


	@Override
	public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
		ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
		PolarisConfigRefreshScopeAnnotationDetector detector = applicationContext.getBean(PolarisConfigRefreshScopeAnnotationDetector.class);
		boolean isRefreshScopeAnnotationUsed = detector.isRefreshScopeAnnotationUsed();
		String annotatedRefreshScopeBeanName = detector.getAnnotatedRefreshScopeBeanName();
		// using System.setProperty to set spring.cloud.polaris.config.refresh-type
		String value = System.getProperty("spring.cloud.polaris.config.refresh-type");
		boolean isSystemSetRefreshType = RefreshType.REFRESH_CONTEXT.toString().equalsIgnoreCase(value);
		// a bean is using @RefreshScope, but the config refresh type is still [reflect], switch automatically
		if (isRefreshScopeAnnotationUsed || isSystemSetRefreshType) {
			if (isRefreshScopeAnnotationUsed) {
				LOGGER.warn("Detected that the bean [{}] is using @RefreshScope annotation, but the config refresh type is still [reflect]. " + "[SCT] will automatically switch to [refresh_context].", annotatedRefreshScopeBeanName);
			}
			if (isSystemSetRefreshType) {
				LOGGER.warn("Detected that using System.setProperty to set spring.cloud.polaris.config.refresh-type = refresh_context, but the config refresh type is still [reflect]. " + "[SCT] will automatically switch to [refresh_context].");
			}
			switchConfigRefreshTypeProperty(applicationContext);
			modifyPolarisConfigPropertiesBean(applicationContext);
			// remove related bean of type [reflect]
			removeRelatedBeansOfReflect(applicationContext);
			// register a new refresher bean of type [refresh_context]
			registerRefresherBeanOfRefreshContext(applicationContext);
			// add the new refresher to context as a listener
			addRefresherBeanAsListener(applicationContext);
		}
	}

	private void switchConfigRefreshTypeProperty(ConfigurableApplicationContext applicationContext) {
		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.addFirst(new MapPropertySource(CONFIG_REFRESH_TYPE_PROPERTY, Collections.singletonMap(POLARIS_CONFIG_REFRESH_TYPE, RefreshType.REFRESH_CONTEXT)));
	}

	private void modifyPolarisConfigPropertiesBean(ConfigurableApplicationContext applicationContext) {
		PolarisConfigProperties polarisConfigProperties = applicationContext.getBean(PolarisConfigProperties.class);
		polarisConfigProperties.setRefreshType(RefreshType.REFRESH_CONTEXT);
	}

	private void removeRelatedBeansOfReflect(ConfigurableApplicationContext applicationContext) {
		try {
			DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
			beanFactory.removeBeanDefinition(REFLECT_REFRESHER_BEAN_NAME);
			beanFactory.removeBeanDefinition(REFLECT_REBINDER_BEAN_NAME);
		}
		catch (BeansException e) {
		    // If there is a removeBean exception in this code, do not affect the main process startup. Some user usage may cause the polarisReflectPropertySourceAutoRefresher to not load, and the removeBeanDefinition will report an error
			LOGGER.debug("removeRelatedBeansOfReflect occur error:", e);
		}
	}

	private void registerRefresherBeanOfRefreshContext(ConfigurableApplicationContext applicationContext) {
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition().getBeanDefinition();
		beanDefinition.setBeanClass(PolarisRefreshEntireContextRefresher.class);
		PolarisConfigProperties polarisConfigProperties = beanFactory.getBean(PolarisConfigProperties.class);
		PolarisPropertySourceManager polarisPropertySourceManager = beanFactory.getBean(PolarisPropertySourceManager.class);
		ContextRefresher contextRefresher = beanFactory.getBean(ContextRefresher.class);
		ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, polarisConfigProperties);
		constructorArgumentValues.addIndexedArgumentValue(1, polarisPropertySourceManager);
		constructorArgumentValues.addIndexedArgumentValue(2, contextRefresher);
		beanFactory.registerBeanDefinition(REFRESH_CONTEXT_REFRESHER_BEAN_NAME, beanDefinition);
	}


	private void addRefresherBeanAsListener(ConfigurableApplicationContext applicationContext) {
		PolarisRefreshEntireContextRefresher refresher = (PolarisRefreshEntireContextRefresher) applicationContext.getBean(REFRESH_CONTEXT_REFRESHER_BEAN_NAME);
		applicationContext.addApplicationListener(refresher);
	}
}
