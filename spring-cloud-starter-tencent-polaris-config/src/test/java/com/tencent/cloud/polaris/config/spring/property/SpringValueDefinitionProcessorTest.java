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

package com.tencent.cloud.polaris.config.spring.property;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import com.tencent.polaris.api.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Test for {@link SpringValueDefinitionProcessor}.
 *
 * @author lingxiao.wlx
 */
public class SpringValueDefinitionProcessorTest {

	@Test
	public void springValueDefinitionProcessorTest() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("bean.xml");
		Person person = context.getBean(Person.class);

		SpringValueRegistry springValueRegistry = context.getBean(SpringValueRegistry.class);

		BeanFactory beanFactory = person.getBeanFactory();
		Collection<SpringValue> name = springValueRegistry.get(beanFactory, "name");
		Assert.assertFalse(CollectionUtils.isEmpty(name));
		Optional<SpringValue> nameSpringValueOptional = name.stream().findAny();
		Assert.assertTrue(nameSpringValueOptional.isPresent());

		SpringValue nameSpringValue = nameSpringValueOptional.get();
		Method method = nameSpringValue.getMethodParameter().getMethod();
		Assert.assertTrue(Objects.nonNull(method));
		Assert.assertEquals("setName", method.getName());
		Assert.assertEquals("${name:test}", nameSpringValue.getPlaceholder());
		Assert.assertFalse(nameSpringValue.isField());
		Assert.assertEquals(String.class, nameSpringValue.getTargetType());


		Collection<SpringValue> age = springValueRegistry.get(beanFactory, "age");
		Assert.assertFalse(CollectionUtils.isEmpty(age));
		Optional<SpringValue> ageSpringValueOptional = age.stream().findAny();
		Assert.assertTrue(ageSpringValueOptional.isPresent());

		SpringValue ageSpringValue = ageSpringValueOptional.get();
		Method method1 = ageSpringValue.getMethodParameter().getMethod();
		Assert.assertTrue(Objects.nonNull(method1));
		Assert.assertEquals("setAge", method1.getName());
		Assert.assertEquals("${age:10}", ageSpringValue.getPlaceholder());
		Assert.assertFalse(ageSpringValue.isField());
		Assert.assertEquals(String.class, ageSpringValue.getTargetType());
	}
}
