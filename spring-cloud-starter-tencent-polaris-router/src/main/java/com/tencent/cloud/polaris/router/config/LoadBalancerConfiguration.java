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

package com.tencent.cloud.polaris.router.config;

import java.util.List;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.router.PolarisRouterServiceInstanceListSupplier;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterResponseInterceptor;
import com.tencent.cloud.rpc.enhancement.transformer.InstanceTransformer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Auto configuration for ribbon components.
 * @author lepdou 2022-05-17
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisRouterEnabled
@ConditionalOnDiscoveryEnabled
public class LoadBalancerConfiguration {

	/**
	 * Order of reactive discovery service instance supplier.
	 */
	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	protected static class PolarisReactiveSupportConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context,
				PolarisSDKContextManager polarisSDKContextManager, List<RouterRequestInterceptor> requestInterceptors,
				List<RouterResponseInterceptor> responseInterceptors, InstanceTransformer instanceTransformer) {
			return new PolarisRouterServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withDiscoveryClient().build(context),
					polarisSDKContextManager.getRouterAPI(),
					requestInterceptors,
					responseInterceptors,
					instanceTransformer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	protected static class PolarisBlockingSupportConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context,
				PolarisSDKContextManager polarisSDKContextManager, List<RouterRequestInterceptor> requestInterceptors,
				List<RouterResponseInterceptor> responseInterceptors, InstanceTransformer instanceTransformer) {
			return new PolarisRouterServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().build(context),
					polarisSDKContextManager.getRouterAPI(),
					requestInterceptors,
					responseInterceptors,
					instanceTransformer);
		}
	}
}
