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

import com.tencent.cloud.constant.LoadbalancerConstant;
import com.tencent.cloud.polaris.PolarisProperties;
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
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
public class PolarisLoadBalancerClientConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReactorLoadBalancer<ServiceInstance> polarisLoadBalancer(Environment environment,
                                                                  LoadBalancerClientFactory loadBalancerClientFactory,
                                                                  PolarisProperties polarisProperties,
                                                                  RouterAPI routerAPI) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new PolarisLoadbalancer(
                name, loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
                polarisProperties, routerAPI);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnReactiveDiscoveryEnabled
    @Order(LoadbalancerConstant.DISCOVERY_SERVICE_INSTANCE_SUPPLIER_ORDER)
    public static class ReactiveSupportConfiguration {

        @Bean
        @ConditionalOnBean(ReactiveDiscoveryClient.class)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "default", matchIfMissing = true)
        public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
                ConfigurableApplicationContext context) {
            return ServiceInstanceListSupplier.builder().withDiscoveryClient()
                    .build(context);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBlockingDiscoveryEnabled
    @Order(LoadbalancerConstant.DISCOVERY_SERVICE_INSTANCE_SUPPLIER_ORDER + 1)
    public static class BlockingSupportConfiguration {

        @Bean
        @ConditionalOnBean(DiscoveryClient.class)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "default", matchIfMissing = true)
        public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
                ConfigurableApplicationContext context) {
            return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
                    .build(context);
        }
    }

}
