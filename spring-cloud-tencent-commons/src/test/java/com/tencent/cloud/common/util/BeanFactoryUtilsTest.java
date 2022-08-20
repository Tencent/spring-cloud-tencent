package com.tencent.cloud.common.util;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

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

		Assert.assertTrue(childBeanFactory.getBeansOfType(Foo.class).isEmpty());
		Assert.assertTrue(BeanFactoryUtils.getBeans(childBeanFactory, Foo.class).size() == 1);

		Assert.assertTrue(BeanFactoryUtils.getBeans(childBeanFactory, Bar.class).isEmpty());
	}

	static class Foo {

	}

	static class Bar {

	}
}
