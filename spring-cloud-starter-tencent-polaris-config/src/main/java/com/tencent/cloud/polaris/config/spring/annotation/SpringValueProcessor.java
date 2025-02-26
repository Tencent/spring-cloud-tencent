/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 */

package com.tencent.cloud.polaris.config.spring.annotation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.CaseFormat;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueDefinition;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;

/**
 * Spring value processor of field or method which has @Value and xml config placeholders.
 * <p>
 * This source file was originally from:
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/annotation/SpringValueProcessor.java>
 *     SpringValueProcessor</a></code>
 *
 * @author weihubeats 2022-7-10
 */
public class SpringValueProcessor extends PolarisProcessor implements BeanDefinitionRegistryPostProcessor, BeanFactoryAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringValueProcessor.class);

	private static final Set<BeanDefinitionRegistry> PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES = Sets.newConcurrentHashSet();
	private static final Map<BeanDefinitionRegistry, Multimap<String, SpringValueDefinition>> BEAN_DEFINITION_REGISTRY_MULTIMAP_CONCURRENT_MAP =
			Maps.newConcurrentMap();
	private final PolarisConfigProperties polarisConfigProperties;
	private final PlaceholderHelper placeholderHelper;
	private final SpringValueRegistry springValueRegistry;

	private BeanFactory beanFactory;
	private Multimap<String, SpringValueDefinition> beanName2SpringValueDefinitions;

	public SpringValueProcessor(PlaceholderHelper placeholderHelper,
			SpringValueRegistry springValueRegistry,
			PolarisConfigProperties polarisConfigProperties) {
		this.placeholderHelper = placeholderHelper;
		this.polarisConfigProperties = polarisConfigProperties;
		this.springValueRegistry = springValueRegistry;
		beanName2SpringValueDefinitions = LinkedListMultimap.create();
	}

	@Override
	public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		if (polarisConfigProperties.isAutoRefresh() && beanFactory instanceof BeanDefinitionRegistry) {
			beanName2SpringValueDefinitions = this.getBeanName2SpringValueDefinitions((BeanDefinitionRegistry) beanFactory);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, @NonNull String beanName)
			throws BeansException {
		if (polarisConfigProperties.isAutoRefresh()) {
			super.postProcessBeforeInitialization(bean, beanName);
			processBeanPropertyValues(bean, beanName);
		}
		return bean;
	}


	@Override
	protected void processField(Object bean, String beanName, Field field, boolean isRefreshScope) {
		// register @Value on field
		Value value = field.getAnnotation(Value.class);
		if (value == null) {
			return;
		}

		doRegister(bean, beanName, field, value, isRefreshScope);
	}

	@Override
	protected void processMethod(Object bean, String beanName, Method method, boolean isRefreshScope) {
		//register @Value on method
		Value value = method.getAnnotation(Value.class);
		if (value != null) {
			processMethodValue(bean, beanName, method, value, isRefreshScope);
			return;
		}

		if (method.getAnnotation(RefreshScope.class) != null) {
			processMethodRefreshScope(bean, method);
		}
	}

	private void processMethodValue(Object bean, String beanName, Method method, Value value, boolean isRefreshScope) {
		//skip Configuration bean methods
		if (method.getAnnotation(Bean.class) != null) {
			return;
		}
		if (method.getParameterTypes().length != 1) {
			LOGGER.error("Ignore @Value setter {}.{}, expecting 1 parameter, actual {} parameters",
					bean.getClass().getName(), method.getName(), method.getParameterTypes().length);
			return;
		}
		doRegister(bean, beanName, method, value, isRefreshScope);
	}

	/**
	 * @RefreshScope on method.
	 * method parameter with @Value ${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestConfig3#testBean3}.
	 * method parameter class with @ConfigurationProperties ${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestConfig4#testBean4}.
	 * @ConfigurationProperties outside method may effect @RefreshScope bean${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestConfig5#testBean5()}.
	 * @param bean spring bean.
	 * @param method method.
	 */
	private void processMethodRefreshScope(Object bean, Method method) {
		// must have @Bean annotation
		if (method.getAnnotation(Bean.class) == null) {
			return;
		}

		for (Parameter parameter : method.getParameters()) {
			Value value = parameter.getAnnotation(Value.class);
			if (value != null) {
				// method parameter with @Value
				Set<String> keys = placeholderHelper.extractPlaceholderKeys(value.value());
				springValueRegistry.putRefreshScopeKeys(keys);
			}
			// method parameter class with @ConfigurationProperties
			ConfigurationProperties configurationProperties = parameter.getType().getAnnotation(ConfigurationProperties.class);
			parseConfigurationPropertiesKeys(configurationProperties, parameter.getType());
		}

		// analyze all fields of the class containing the method.
		for (Field field : findAllField(bean.getClass())) {
			Value value = field.getAnnotation(Value.class);
			if (value != null) {
				// field with @Value
				Set<String> keys = placeholderHelper.extractPlaceholderKeys(value.value());
				springValueRegistry.putRefreshScopeKeys(keys);
				continue;
			}
			// field class with @ConfigurationProperties
			ConfigurationProperties configurationProperties = field.getType().getAnnotation(ConfigurationProperties.class);
			parseConfigurationPropertiesKeys(configurationProperties, field.getType());
		}
	}

	/**
	 * parse refresh scope keys from @ConfigurationProperties.
	 * @param configurationProperties @ConfigurationProperties annotation object.
	 * @param clazz class of @ConfigurationProperties bean.
	 */
	private void parseConfigurationPropertiesKeys(ConfigurationProperties configurationProperties, Class<?> clazz) {
		if (configurationProperties != null) {
			// get prefix from @ConfigurationProperties prefix or value.
			String prefix = configurationProperties.value();
			if (StringUtils.isEmpty(prefix)) {
				prefix = configurationProperties.prefix();
			}
			if (StringUtils.isNotEmpty(prefix)) {
				prefix += ".";
			}
			parseConfigKeys(clazz, prefix);
		}
	}

	/**
	 * parse all fields of the configClazz.
	 * if the field is primitive or wrapper, add it to refresh scope key map.
	 * ${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestBeanProperties2#name}
	 * if the field is collection, add it to refresh scope prefix trie node.
	 * ${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestBeanProperties2#list}
	 * if the field is complex type, recursive parse.
	 * ${@link com.tencent.cloud.polaris.config.spring.annotation.RefreshScopeSpringProcessorTest.TestBeanProperties2#inner}
	 * @param configClazz class or subclass of @ConfigurationProperties bean.
	 * @param prefix prefix or subclass's prefix of @ConfigurationProperties bean.
	 */
	private void parseConfigKeys(Class<?> configClazz, String prefix) {
		for (Field field : findAllField(configClazz)) {
			if (isPrimitiveOrWrapper(field.getType())) {
				// lowerCamel format
				springValueRegistry.putRefreshScopeKey(prefix + field.getName());
				// lower-hyphen format
				springValueRegistry.putRefreshScopeKey(
						prefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()));
			}
			else if (isCollection(field.getType())) {
				springValueRegistry.putRefreshScopePrefixKey(prefix + field.getName());
				springValueRegistry.putRefreshScopePrefixKey(
						prefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()));
			}
			else {
				// complex type, recursive parse
				parseConfigKeys(field.getType(), prefix + field.getName() + ".");
				parseConfigKeys(field.getType(),
						prefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, field.getName()) + ".");
			}
		}
	}

	/**
	 * whether the class is primitive or wrapper.
	 * @param clazz the class under analysis.
	 * @return true if the class is primitive or wrapper, otherwise false.
	 */
	private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		return clazz.isPrimitive() ||
				clazz == String.class ||
				clazz == Boolean.class ||
				clazz == Character.class ||
				clazz == Byte.class ||
				clazz == Short.class ||
				clazz == Integer.class ||
				clazz == Long.class ||
				clazz == Float.class ||
				clazz == Double.class;
	}

	/**
	 * whether the class is collection(array, collection, map).
	 * @param clazz the class under analysis.
	 * @return true if the class is collection(array, collection, map), otherwise false.
	 */
	private static boolean isCollection(Class<?> clazz) {
		return clazz.isArray() || Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
		if (polarisConfigProperties.isAutoRefresh()) {
			processPropertyValues(beanDefinitionRegistry);
		}
	}

	private void doRegister(Object bean, String beanName, Member member, Value value, boolean isRefreshScope) {
		Set<String> keys = placeholderHelper.extractPlaceholderKeys(value.value());
		if (keys.isEmpty()) {
			return;
		}

		for (String key : keys) {
			SpringValue springValue;
			if (member instanceof Field) {
				Field field = (Field) member;
				springValue = new SpringValue(key, value.value(), bean, beanName, field);
				if (isRefreshScope) {
					springValueRegistry.putRefreshScopeKey(key);
				}
			}
			else if (member instanceof Method) {
				Method method = (Method) member;
				springValue = new SpringValue(key, value.value(), bean, beanName, method);
				if (isRefreshScope) {
					springValueRegistry.putRefreshScopeKey(key);
				}
			}
			else {
				LOGGER.error("Polaris @Value annotation currently only support to be used on methods and fields, "
						+ "but is used on {}", member.getClass());
				return;
			}
			springValueRegistry.register(beanFactory, key, springValue);
			LOGGER.debug("Monitoring {}", springValue);
		}
	}

	private void processBeanPropertyValues(Object bean, String beanName) {
		Collection<SpringValueDefinition> propertySpringValues = beanName2SpringValueDefinitions.get(beanName);
		if (propertySpringValues.isEmpty()) {
			return;
		}

		for (SpringValueDefinition definition : propertySpringValues) {
			try {
				PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(bean.getClass(), definition.getPropertyName());
				if (pd != null) {
					Method method = pd.getWriteMethod();
					if (method == null) {
						continue;
					}
					SpringValue springValue = new SpringValue(definition.getKey(), definition.getPlaceholder(),
							bean, beanName, method);
					springValueRegistry.register(beanFactory, definition.getKey(), springValue);
					LOGGER.debug("Monitoring {}", springValue);
				}
			}
			catch (Throwable ex) {
				LOGGER.error("Failed to enable auto update feature for {}.{}", bean.getClass(),
						definition.getPropertyName());
			}
		}

		// clear
		beanName2SpringValueDefinitions.removeAll(beanName);
	}

	private Multimap<String, SpringValueDefinition> getBeanName2SpringValueDefinitions(BeanDefinitionRegistry registry) {
		Multimap<String, SpringValueDefinition> springValueDefinitions = BEAN_DEFINITION_REGISTRY_MULTIMAP_CONCURRENT_MAP.remove(registry);
		if (springValueDefinitions == null) {
			springValueDefinitions = LinkedListMultimap.create();
		}
		return springValueDefinitions;
	}

	private void processPropertyValues(BeanDefinitionRegistry beanRegistry) {
		if (!PROPERTY_VALUES_PROCESSED_BEAN_FACTORIES.add(beanRegistry)) {
			// already initialized
			return;
		}

		if (!BEAN_DEFINITION_REGISTRY_MULTIMAP_CONCURRENT_MAP.containsKey(beanRegistry)) {
			BEAN_DEFINITION_REGISTRY_MULTIMAP_CONCURRENT_MAP.put(beanRegistry, LinkedListMultimap.create());
		}

		Multimap<String, SpringValueDefinition> springValueDefinitions = BEAN_DEFINITION_REGISTRY_MULTIMAP_CONCURRENT_MAP.get(beanRegistry);

		String[] beanNames = beanRegistry.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanRegistry.getBeanDefinition(beanName);
			MutablePropertyValues mutablePropertyValues = beanDefinition.getPropertyValues();
			List<PropertyValue> propertyValues = mutablePropertyValues.getPropertyValueList();
			for (PropertyValue propertyValue : propertyValues) {
				Object value = propertyValue.getValue();
				if (!(value instanceof TypedStringValue)) {
					continue;
				}
				String placeholder = ((TypedStringValue) value).getValue();
				Set<String> keys = placeholderHelper.extractPlaceholderKeys(placeholder);

				if (keys.isEmpty()) {
					continue;
				}

				for (String key : keys) {
					springValueDefinitions.put(beanName, new SpringValueDefinition(key, placeholder, propertyValue.getName()));
				}
			}
		}
	}
}
