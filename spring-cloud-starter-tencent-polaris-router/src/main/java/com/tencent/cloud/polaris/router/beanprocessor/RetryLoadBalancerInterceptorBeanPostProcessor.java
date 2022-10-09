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

package com.tencent.cloud.polaris.router.beanprocessor;

import com.tencent.cloud.polaris.router.resttemplate.PolarisRetryLoadBalancerInterceptor;
import com.tencent.cloud.polaris.router.resttemplate.RouterContextFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.lang.NonNull;

/**
 * Replace {@link RetryLoadBalancerInterceptor}RetryLoadBalancerInterceptor with {@link PolarisRetryLoadBalancerInterceptor}.
 * PolarisRetryLoadBalancerInterceptor can pass routing context information.
 *
 * @author lepdou 2022-10-09
 */
public class RetryLoadBalancerInterceptorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory factory;

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof RetryLoadBalancerInterceptor) {
			// Support rest template router.
			// Replaces the default RetryLoadBalancerInterceptor implementation
			// and returns a custom PolarisRetryLoadBalancerInterceptor

			LoadBalancerClient loadBalancerClient = this.factory.getBean(LoadBalancerClient.class);
			LoadBalancerRetryProperties lbProperties = this.factory.getBean(LoadBalancerRetryProperties.class);
			LoadBalancerRequestFactory requestFactory = this.factory.getBean(LoadBalancerRequestFactory.class);
			LoadBalancedRetryFactory lbRetryFactory = this.factory.getBean(LoadBalancedRetryFactory.class);
			RouterContextFactory routerContextFactory = this.factory.getBean(RouterContextFactory.class);

			return new PolarisRetryLoadBalancerInterceptor(loadBalancerClient, lbProperties, requestFactory, lbRetryFactory,
					routerContextFactory);
		}
		return bean;
	}
}
