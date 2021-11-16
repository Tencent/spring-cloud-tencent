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

package com.tencent.cloud.polaris.router;

import com.tencent.cloud.constant.LoadbalancerConstant;
import com.tencent.cloud.polaris.router.PolarisRouterServiceInstanceListSupplier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.RouterAPIFactory;
import com.tencent.polaris.router.api.core.RouterAPI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration Ribbon for Polaris.
 *
 * @author Haotian Zhang
 */
@Configuration()
@EnableConfigurationProperties
@ConditionalOnProperty(value = "spring.cloud.polaris.loadbalancer.enabled", matchIfMissing = true)
public class PolarisRouterAutoConfiguration {

    @Bean(name = "polarisRoute")
    @ConditionalOnMissingBean
    public RouterAPI polarisRouter(SDKContext polarisContext) throws PolarisException {
        return RouterAPIFactory.createRouterAPIByContext(polarisContext);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnReactiveDiscoveryEnabled
    @Order(LoadbalancerConstant.ROUTER_SERVICE_INSTANCE_SUPPLIER_ORDER - 1000)
    public static class ReactiveSupportConfiguration {

//        @Bean
//        @ConditionalOnBean(ReactiveDiscoveryClient.class)
//        @ConditionalOnMissingBean
//        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "default", matchIfMissing = true)
//        public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(RouterAPI routerAPI,
//                ConfigurableApplicationContext context) {
//            return new PolarisRouterServiceInstanceListSupplier(ServiceInstanceListSupplier.builder().withDiscoveryClient()
//                    .build(context), routerAPI);
//        }

        @Bean
        @ConditionalOnBean(ReactiveDiscoveryClient.class)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "zone-preference", matchIfMissing = true)
        public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(RouterAPI routerAPI,
                                                                                      ConfigurableApplicationContext context) {
            return new PolarisRouterServiceInstanceListSupplier(ServiceInstanceListSupplier.builder().withDiscoveryClient()
                    .withZonePreference()
                    .build(context), routerAPI);
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBlockingDiscoveryEnabled
    @Order(LoadbalancerConstant.ROUTER_SERVICE_INSTANCE_SUPPLIER_ORDER + 1 - 1000)
    public static class BlockingSupportConfiguration {

//        @Bean
//        @ConditionalOnBean(DiscoveryClient.class)
//        @ConditionalOnMissingBean
//        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "default", matchIfMissing = true)
//        public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
//                RouterAPI routerAPI,
//                ConfigurableApplicationContext context) {
//            return new PolarisRouterServiceInstanceListSupplier(ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
//                    .build(context), routerAPI);
//        }

        @Bean
        @ConditionalOnBean(DiscoveryClient.class)
        @ConditionalOnMissingBean
        @ConditionalOnProperty(value = "spring.cloud.loadbalancer.configurations", havingValue = "zone-preference", matchIfMissing = true)
        public ServiceInstanceListSupplier polarisRouterDiscoveryClientServiceInstanceListSupplier(RouterAPI routerAPI,
                                                                                                    ConfigurableApplicationContext context) {
            return new PolarisRouterServiceInstanceListSupplier(ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
                    .withZonePreference().build(context), routerAPI);
        }

    }

}
