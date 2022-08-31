package com.tencent.cloud.polaris.config.spring.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

/**
 * Get spring bean properties and methods.
 *
 * @author weihubeats 2022-7-10
 */
public abstract class PolarisProcessor implements BeanPostProcessor, PriorityOrdered {

	@Override
	public Object postProcessBeforeInitialization(Object bean, @NonNull String beanName)
			throws BeansException {
		Class<?> clazz = bean.getClass();
		for (Field field : findAllField(clazz)) {
			processField(bean, beanName, field);
		}
		for (Method method : findAllMethod(clazz)) {
			processMethod(bean, beanName, method);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		return bean;
	}

	/**
	 * subclass should implement this method to process field.
	 * @param bean bean
	 * @param beanName beanName
	 * @param field field
	 */
	protected abstract void processField(Object bean, String beanName, Field field);

	/**
	 * subclass should implement this method to process method.
	 * @param bean bean
	 * @param beanName beanName
	 * @param method method
	 */
	protected abstract void processMethod(Object bean, String beanName, Method method);


	@Override
	public int getOrder() {
		//make it as late as possible
		return Ordered.LOWEST_PRECEDENCE;
	}

	private List<Field> findAllField(Class<?> clazz) {
		final List<Field> res = new LinkedList<>();
		ReflectionUtils.doWithFields(clazz, res::add);
		return res;
	}

	private List<Method> findAllMethod(Class<?> clazz) {
		final List<Method> res = new LinkedList<>();
		ReflectionUtils.doWithMethods(clazz, res::add);
		return res;
	}
}
