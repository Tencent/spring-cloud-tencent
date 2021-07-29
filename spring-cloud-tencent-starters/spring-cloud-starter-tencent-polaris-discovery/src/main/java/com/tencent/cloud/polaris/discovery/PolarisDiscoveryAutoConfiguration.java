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

package com.tencent.cloud.polaris.discovery;

import com.tencent.cloud.polaris.PolarisProperties;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClientConfiguration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Discovery Auto Configuration for Polaris.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisDiscoveryEnabled
@Import({PolarisDiscoveryClientConfiguration.class, PolarisReactiveDiscoveryClientConfiguration.class})
public class PolarisDiscoveryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PolarisProperties polarisDiscoveryProperties() {
        return new PolarisProperties();
    }

    @Bean(name = "polarisProvider")
    @ConditionalOnMissingBean
    public ProviderAPI polarisProvider(SDKContext polarisContext) throws PolarisException {
        return DiscoveryAPIFactory.createProviderAPIByContext(polarisContext);
    }

    @Bean(name = "polarisConsumer")
    @ConditionalOnMissingBean
    public ConsumerAPI polarisConsumer(SDKContext polarisContext) throws PolarisException {
        return DiscoveryAPIFactory.createConsumerAPIByContext(polarisContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public PolarisDiscoveryHandler polarisDiscoveryHandler() {
        return new PolarisDiscoveryHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolarisServiceDiscovery polarisServiceDiscovery(PolarisDiscoveryHandler polarisDiscoveryHandler) {
        return new PolarisServiceDiscovery(polarisDiscoveryHandler);
    }
}
