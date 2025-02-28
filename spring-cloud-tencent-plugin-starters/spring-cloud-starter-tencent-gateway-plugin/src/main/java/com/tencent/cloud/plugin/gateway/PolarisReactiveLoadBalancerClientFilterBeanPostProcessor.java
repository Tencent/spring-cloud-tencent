/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.plugin.gateway;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

public class PolarisReactiveLoadBalancerClientFilterBeanPostProcessor implements BeanPostProcessor, Ordered {
	/**
	 * Order of this bean post processor.
	 */
	public static final int ORDER = 0;

	private ApplicationContext applicationContext;

	public PolarisReactiveLoadBalancerClientFilterBeanPostProcessor(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ReactiveLoadBalancerClientFilter && !(bean instanceof PolarisReactiveLoadBalancerClientFilter)) {
			LoadBalancerClientFactory clientFactory = applicationContext.getBean(LoadBalancerClientFactory.class);
			GatewayLoadBalancerProperties properties = applicationContext.getBean(GatewayLoadBalancerProperties.class);

			return new PolarisReactiveLoadBalancerClientFilter(clientFactory, properties, (ReactiveLoadBalancerClientFilter) bean);
		}
		return bean;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}
}
