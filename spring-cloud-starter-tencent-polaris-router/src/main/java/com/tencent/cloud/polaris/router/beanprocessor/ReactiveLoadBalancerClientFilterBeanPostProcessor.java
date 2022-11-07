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

package com.tencent.cloud.polaris.router.beanprocessor;

import java.util.List;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.BeanFactoryUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.scg.PolarisReactiveLoadBalancerClientFilter;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

/**
 * Replaced ReactiveLoadBalancerClientFilter with PolarisReactiveLoadBalancerClientFilter during creating bean phase.
 *@author lepdou 2022-06-20
 */
public class ReactiveLoadBalancerClientFilterBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory factory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// Support spring cloud gateway router.
		// Replaces the default ReactiveLoadBalancerClientFilter implementation
		// and returns a custom PolarisReactiveLoadBalancerClientFilter
		if (bean instanceof ReactiveLoadBalancerClientFilter) {
			LoadBalancerClientFactory loadBalancerClientFactory = this.factory.getBean(LoadBalancerClientFactory.class);
			GatewayLoadBalancerProperties gatewayLoadBalancerProperties = this.factory.getBean(GatewayLoadBalancerProperties.class);
			List<SpringWebRouterLabelResolver> routerLabelResolvers = BeanFactoryUtils.getBeans(factory, SpringWebRouterLabelResolver.class);
			StaticMetadataManager staticMetadataManager = this.factory.getBean(StaticMetadataManager.class);
			RouterRuleLabelResolver routerRuleLabelResolver = this.factory.getBean(RouterRuleLabelResolver.class);
			PolarisContextProperties polarisContextProperties = this.factory.getBean(PolarisContextProperties.class);

			return new PolarisReactiveLoadBalancerClientFilter(
					loadBalancerClientFactory, gatewayLoadBalancerProperties,
					staticMetadataManager, routerRuleLabelResolver, routerLabelResolvers, polarisContextProperties);
		}
		return bean;
	}
}
