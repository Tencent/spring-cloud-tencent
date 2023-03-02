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

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.BeanFactoryUtils;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.scg.PolarisReactiveLoadBalancerClientFilter;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


/**
 * Test for ${@link ReactiveLoadBalancerClientFilterBeanPostProcessor}.
 *
 * @author lepdou 2022-07-04
 */
@ExtendWith(MockitoExtension.class)
public class ReactiveLoadBalancerClientFilterBeanPostProcessorTest {

	@Mock
	private BeanFactory beanFactory;
	@Mock
	private LoadBalancerClientFactory loadBalancerClientFactory;
	@Mock
	private GatewayLoadBalancerProperties gatewayLoadBalancerProperties;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;

	@Test
	public void testWrapReactiveLoadBalancerClientFilter() {
		when(beanFactory.getBean(LoadBalancerClientFactory.class)).thenReturn(loadBalancerClientFactory);
		when(beanFactory.getBean(GatewayLoadBalancerProperties.class)).thenReturn(gatewayLoadBalancerProperties);
		when(beanFactory.getBean(StaticMetadataManager.class)).thenReturn(staticMetadataManager);
		when(beanFactory.getBean(RouterRuleLabelResolver.class)).thenReturn(routerRuleLabelResolver);

		try (MockedStatic<BeanFactoryUtils> mockedBeanFactoryUtils = Mockito.mockStatic(BeanFactoryUtils.class)) {
			mockedBeanFactoryUtils.when(() -> BeanFactoryUtils.getBeans(beanFactory, SpringWebRouterLabelResolver.class))
					.thenReturn(null);

			ReactiveLoadBalancerClientFilter reactiveLoadBalancerClientFilter = new ReactiveLoadBalancerClientFilter(
					loadBalancerClientFactory, gatewayLoadBalancerProperties);

			ReactiveLoadBalancerClientFilterBeanPostProcessor processor = new ReactiveLoadBalancerClientFilterBeanPostProcessor();
			processor.setBeanFactory(beanFactory);

			Object bean = processor.postProcessBeforeInitialization(reactiveLoadBalancerClientFilter, "");
			assertThat(bean).isInstanceOf(PolarisReactiveLoadBalancerClientFilter.class);
		}
	}

	@Test
	public void testNotWrapLoadBalancerInterceptor() {
		ReactiveLoadBalancerClientFilterBeanPostProcessor processor = new ReactiveLoadBalancerClientFilterBeanPostProcessor();
		processor.setBeanFactory(beanFactory);

		OtherBean otherBean = new OtherBean();
		Object bean = processor.postProcessBeforeInitialization(otherBean, "");
		assertThat(bean).isNotInstanceOf(PolarisReactiveLoadBalancerClientFilter.class);
		assertThat(bean).isInstanceOf(OtherBean.class);
	}

	static class OtherBean {

	}
}
