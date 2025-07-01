/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.router.beanprocessor;

import com.tencent.cloud.polaris.router.instrument.resttemplate.PolarisRetryLoadBalancerInterceptor;
import com.tencent.cloud.polaris.router.instrument.resttemplate.RouterContextFactory;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Replace {@link RetryLoadBalancerInterceptor}RetryLoadBalancerInterceptor with {@link PolarisRetryLoadBalancerInterceptor}.
 * PolarisRetryLoadBalancerInterceptor can pass routing context information.
 *
 * @author lepdou 2022-10-09
 */
public class RetryLoadBalancerInterceptorBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {
	/**
	 * The order of the bean post processor. if user want to wrap it(CustomLoadBalancerInterceptor -> PolarisRetryLoadBalancerInterceptor), CustomLoadBalancerInterceptorBeanPostProcessor's order should be bigger than ${@link POLARIS_RETRY_LOAD_BALANCER_INTERCEPTOR_POST_PROCESSOR_ORDER}.
	 */
	public static final int POLARIS_RETRY_LOAD_BALANCER_INTERCEPTOR_POST_PROCESSOR_ORDER = 0;

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
			EnhancedPluginRunner pluginRunner = this.factory.getBean(EnhancedPluginRunner.class);

			return new PolarisRetryLoadBalancerInterceptor(loadBalancerClient, lbProperties, requestFactory, lbRetryFactory,
					routerContextFactory, pluginRunner);
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return POLARIS_RETRY_LOAD_BALANCER_INTERCEPTOR_POST_PROCESSOR_ORDER;
	}
}
