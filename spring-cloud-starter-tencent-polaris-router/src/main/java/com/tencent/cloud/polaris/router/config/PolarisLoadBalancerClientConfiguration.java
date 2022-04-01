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

package com.tencent.cloud.polaris.router.config;

import com.tencent.cloud.polaris.router.PolarisRouterServiceInstanceListSupplier;
import com.tencent.cloud.polaris.router.PolarisRoutingLoadbalancer;
import com.tencent.polaris.router.api.core.RouterAPI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * Configuration of loadbalancer client.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
public class PolarisLoadBalancerClientConfiguration {

	/**
	 * Order of reactive discovery service instance supplier.
	 */
	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;


	@Bean
	@ConditionalOnMissingBean
	public ReactorLoadBalancer<ServiceInstance> polarisLoadBalancer(Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory, PolarisLoadBalancerProperties loadBalancerProperties,
			RouterAPI routerAPI) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		return new PolarisRoutingLoadbalancer(name,
				loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
				loadBalancerProperties, routerAPI);
	}

	@Configuration
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	static class PolarisReactiveSupportConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "polaris")
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(RouterAPI routerAPI,
				ConfigurableApplicationContext context) {
			return new PolarisRouterServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withDiscoveryClient().build(context), routerAPI);
		}

	}

	@Configuration
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	static class PolarisBlockingSupportConfiguration {

		@Bean
		@ConditionalOnBean(DiscoveryClient.class)
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "polaris")
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(RouterAPI routerAPI,
				ConfigurableApplicationContext context) {
			return new PolarisRouterServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().build(context), routerAPI);
		}

	}

}
