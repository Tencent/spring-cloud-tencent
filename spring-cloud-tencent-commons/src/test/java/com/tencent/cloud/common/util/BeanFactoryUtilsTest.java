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
 */

package com.tencent.cloud.common.util;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link BeanFactoryUtils}.
 *
 * @author Derek Yi 2022-08-18
 */
public class BeanFactoryUtilsTest {

	@Test
	public void testGetBeansIncludingAncestors() {
		DefaultListableBeanFactory parentBeanFactory = new DefaultListableBeanFactory();
		parentBeanFactory.registerBeanDefinition("foo", new RootBeanDefinition(Foo.class));

		DefaultListableBeanFactory childBeanFactory = new DefaultListableBeanFactory(parentBeanFactory);

		assertThat(childBeanFactory.getBeansOfType(Foo.class)).isEmpty();
		assertThat(BeanFactoryUtils.getBeans(childBeanFactory, Foo.class).size()).isEqualTo(1);
		assertThat(BeanFactoryUtils.getBeans(childBeanFactory, Bar.class)).isEmpty();

		MockBeanFactory mockBeanFactory = new MockBeanFactory();
		assertThatThrownBy(() -> BeanFactoryUtils.getBeans(mockBeanFactory, Bar.class))
				.isExactlyInstanceOf(RuntimeException.class)
				.hasMessageContaining("bean factory not support get list bean.");
	}

	static class Foo {

	}

	static class Bar {

	}

	static class MockBeanFactory implements BeanFactory {

		@Override
		public Object getBean(String s) throws BeansException {
			return null;
		}

		@Override
		public <T> T getBean(String s, Class<T> aClass) throws BeansException {
			return null;
		}

		@Override
		public Object getBean(String s, Object... objects) throws BeansException {
			return null;
		}

		@Override
		public <T> T getBean(Class<T> aClass) throws BeansException {
			return null;
		}

		@Override
		public <T> T getBean(Class<T> aClass, Object... objects) throws BeansException {
			return null;
		}

		@Override
		public <T> ObjectProvider<T> getBeanProvider(Class<T> aClass) {
			return null;
		}

		@Override
		public <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
			return null;
		}

		@Override
		public boolean containsBean(String s) {
			return false;
		}

		@Override
		public boolean isSingleton(String s) throws NoSuchBeanDefinitionException {
			return false;
		}

		@Override
		public boolean isPrototype(String s) throws NoSuchBeanDefinitionException {
			return false;
		}

		@Override
		public boolean isTypeMatch(String s, ResolvableType resolvableType) throws NoSuchBeanDefinitionException {
			return false;
		}

		@Override
		public boolean isTypeMatch(String s, Class<?> aClass) throws NoSuchBeanDefinitionException {
			return false;
		}

		@Override
		public Class<?> getType(String s) throws NoSuchBeanDefinitionException {
			return null;
		}

		@Override
		public Class<?> getType(String s, boolean b) throws NoSuchBeanDefinitionException {
			return null;
		}

		@Override
		public String[] getAliases(String s) {
			return new String[0];
		}
	}
}
