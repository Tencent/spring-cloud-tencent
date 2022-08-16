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

import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.tencent.cloud.common.util.BeanFactoryUtils;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.PolarisLoadBalancerCompositeRule;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterResponseInterceptor;
import com.tencent.polaris.router.api.core.RouterAPI;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

/**
 * Decorate IRule with PolarisLoadBalancerCompositeRule.
 *
 * @author derekyi 2022-08-01
 */
public class PolarisLoadBalancerCompositeRuleBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof AbstractLoadBalancerRule) {
			RouterAPI routerAPI = beanFactory.getBean(RouterAPI.class);
			PolarisLoadBalancerProperties polarisLoadBalancerProperties = beanFactory.getBean(PolarisLoadBalancerProperties.class);
			IClientConfig iClientConfig = beanFactory.getBean(IClientConfig.class);
			List<RouterRequestInterceptor> requestInterceptors = BeanFactoryUtils.getBeans(beanFactory, RouterRequestInterceptor.class);
			List<RouterResponseInterceptor> responseInterceptors = BeanFactoryUtils.getBeans(beanFactory, RouterResponseInterceptor.class);
			return new PolarisLoadBalancerCompositeRule(routerAPI, polarisLoadBalancerProperties, iClientConfig,
					requestInterceptors, responseInterceptors, ((AbstractLoadBalancerRule) bean));
		}

		return bean;
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
