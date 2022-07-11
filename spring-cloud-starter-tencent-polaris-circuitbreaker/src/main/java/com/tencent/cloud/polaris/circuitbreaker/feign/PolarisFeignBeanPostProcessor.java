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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import feign.Client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.lang.NonNull;

/**
 * Wrap Spring Bean and decorating proxy for Feign Client.
 *
 * @author Haotian Zhang
 */
public class PolarisFeignBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private final ConsumerAPI consumerAPI;

	private BeanFactory factory;

	public PolarisFeignBeanPostProcessor(ConsumerAPI consumerAPI) {
		this.consumerAPI = consumerAPI;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		return wrapper(bean);
	}

	private Object wrapper(Object bean) {
		if (isNeedWrap(bean)) {
			if (bean instanceof LoadBalancerFeignClient) {
				LoadBalancerFeignClient client = ((LoadBalancerFeignClient) bean);
				return new PolarisLoadBalancerFeignClient(
						createPolarisFeignClient(client.getDelegate()),
						factory(),
						clientFactory());
			}
			if (bean instanceof FeignBlockingLoadBalancerClient) {
				FeignBlockingLoadBalancerClient client = (FeignBlockingLoadBalancerClient) bean;
				return new PolarisFeignBlockingLoadBalancerClient(
						createPolarisFeignClient(client.getDelegate()),
						factory.getBean(BlockingLoadBalancerClient.class));
			}
			return createPolarisFeignClient((Client) bean);
		}
		return bean;
	}

	private boolean isNeedWrap(Object bean) {
		return bean instanceof Client && !(bean instanceof PolarisFeignClient)
				&& !(bean instanceof PolarisFeignBlockingLoadBalancerClient)
				&& !(bean instanceof PolarisLoadBalancerFeignClient);
	}

	private PolarisFeignClient createPolarisFeignClient(Client delegate) {
		return new PolarisFeignClient(delegate, consumerAPI, factory.getBean(PolarisCircuitBreakerProperties.class));
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	CachingSpringLoadBalancerFactory factory() {
		return this.factory.getBean(CachingSpringLoadBalancerFactory.class);
	}

	SpringClientFactory clientFactory() {
		return this.factory.getBean(SpringClientFactory.class);
	}
}
