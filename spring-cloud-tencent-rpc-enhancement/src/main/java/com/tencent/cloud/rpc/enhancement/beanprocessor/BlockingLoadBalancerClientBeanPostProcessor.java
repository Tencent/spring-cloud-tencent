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

package com.tencent.cloud.rpc.enhancement.beanprocessor;

import com.tencent.cloud.rpc.enhancement.instrument.resttemplate.PolarisBlockingLoadBalancerClient;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * BlockingLoadBalancerClientBeanPostProcessor is used to wrap the default BlockingLoadBalancerClient implementation and returns a custom PolarisBlockingLoadBalancerClient.
 *
 * @author Shedfree Wu
 */
public class BlockingLoadBalancerClientBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {
	/**
	 * The order of the bean post processor. If user wants to wrap it(CustomBlockingLoadBalancerClient -> PolarisBlockingLoadBalancerClient), CustomBlockingLoadBalancerClientBeanPostProcessor's order should be bigger than ${@link ORDER}.
	 */
	public static final int ORDER = 0;

	private BeanFactory factory;

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof BlockingLoadBalancerClient) {
			BlockingLoadBalancerClient delegate = (BlockingLoadBalancerClient) bean;
			ReactiveLoadBalancer.Factory<ServiceInstance> requestFactory = this.factory.getBean(ReactiveLoadBalancer.Factory.class);
			EnhancedPluginRunner pluginRunner = this.factory.getBean(EnhancedPluginRunner.class);
			return new PolarisBlockingLoadBalancerClient(requestFactory, delegate, pluginRunner);
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}
}
