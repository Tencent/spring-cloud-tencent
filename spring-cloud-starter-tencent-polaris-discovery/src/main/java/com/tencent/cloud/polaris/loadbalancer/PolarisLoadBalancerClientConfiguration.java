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

package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.loadbalancer.reactive.PolarisLoadBalancerClientRequestTransformer;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
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
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
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
@ConditionalOnPolarisEnabled
@ConditionalOnDiscoveryEnabled
public class PolarisLoadBalancerClientConfiguration {

	/**
	 * Order of reactive discovery service instance supplier.
	 */
	private static final int REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 193827465;

	private final static String STRATEGY_WEIGHT = "polarisWeighted";

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.cloud.polaris.loadbalancer.strategy", havingValue = STRATEGY_WEIGHT)
	public ReactorLoadBalancer<ServiceInstance> polarisLoadBalancer(Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory, RouterAPI routerAPI) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		return new PolarisLoadBalancer(name,
				loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), routerAPI);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnReactiveDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
	protected static class PolarisReactiveSupportConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(ReactiveDiscoveryClient.class)
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return new PolarisServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withDiscoveryClient().build(context));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBlockingDiscoveryEnabled
	@Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
	protected static class PolarisBlockingSupportConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(DiscoveryClient.class)
		public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(
				ConfigurableApplicationContext context) {
			return new PolarisServiceInstanceListSupplier(
					ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().build(context));
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public LoadBalancerClientRequestTransformer polarisLoadBalancerClientRequestTransformer(
			SDKContext sdkContext) {
		ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(sdkContext);
		return new PolarisLoadBalancerClientRequestTransformer(consumerAPI);
	}
}
