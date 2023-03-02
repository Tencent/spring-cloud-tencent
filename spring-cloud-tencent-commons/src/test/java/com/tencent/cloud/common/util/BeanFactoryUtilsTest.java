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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

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
	}

	static class Foo {

	}

	static class Bar {

	}
}
