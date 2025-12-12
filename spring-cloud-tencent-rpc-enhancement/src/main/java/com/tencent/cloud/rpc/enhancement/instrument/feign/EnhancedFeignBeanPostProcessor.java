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

package com.tencent.cloud.rpc.enhancement.instrument.feign;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementProperties;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginRunner;
import feign.Client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;

/**
 * Wrap Spring Bean and decorating proxy for Feign Client.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private final EnhancedPluginRunner pluginRunner;

	private final RpcEnhancementProperties rpcEnhancementProperties;

	private BeanFactory factory;

	public EnhancedFeignBeanPostProcessor(EnhancedPluginRunner pluginRunner, RpcEnhancementProperties rpcEnhancementProperties) {
		this.pluginRunner = pluginRunner;
		this.rpcEnhancementProperties = rpcEnhancementProperties;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return wrapper(bean);
	}

	private Object wrapper(Object bean) {
		if (isNeedWrap(bean)) {
			if (bean instanceof RetryableFeignBlockingLoadBalancerClient
					|| bean instanceof FeignBlockingLoadBalancerClient) {
				Client delegate;
				if (bean instanceof RetryableFeignBlockingLoadBalancerClient) {
					delegate = ((RetryableFeignBlockingLoadBalancerClient) bean).getDelegate();
					return new RetryableFeignBlockingLoadBalancerClient(
							createPolarisFeignClient(delegate),
							factory.getBean(LoadBalancerClient.class),
							factory.getBean(LoadBalancedRetryFactory.class),
							factory.getBean(LoadBalancerClientFactory.class),
							factory.getBeanProvider(LoadBalancerFeignRequestTransformer.class).stream().toList());
				}
				else {
					delegate = ((FeignBlockingLoadBalancerClient) bean).getDelegate();
					return new FeignBlockingLoadBalancerClient(createPolarisFeignClient(delegate),
							factory.getBean(BlockingLoadBalancerClient.class),
							factory.getBean(LoadBalancerClientFactory.class),
							factory.getBeanProvider(LoadBalancerFeignRequestTransformer.class).stream().toList()
					);
				}
			}
			return createPolarisFeignClient((Client) bean);
		}
		return bean;
	}

	private boolean isNeedWrap(Object bean) {
		return bean instanceof Client && !(bean instanceof EnhancedFeignClient);
	}

	private EnhancedFeignClient createPolarisFeignClient(Client delegate) {
		return new EnhancedFeignClient(delegate, pluginRunner, rpcEnhancementProperties);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}
}
