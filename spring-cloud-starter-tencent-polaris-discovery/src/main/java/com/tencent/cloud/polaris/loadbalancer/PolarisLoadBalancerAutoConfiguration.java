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

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.rpc.enhancement.resttemplate.EnhancedRestTemplateInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * Auto-configuration of loadbalancer for Polaris.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnDiscoveryEnabled
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(value = "spring.cloud.polaris.loadbalancer.enabled", matchIfMissing = true)
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@LoadBalancerClients(defaultConfiguration = PolarisLoadBalancerClientConfiguration.class)
public class PolarisLoadBalancerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RestTemplateCustomizer restTemplateCustomizer(@Autowired(required = false) LoadBalancerInterceptor loadBalancerInterceptor) {
		return restTemplate -> {
			List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
			// LoadBalancerInterceptor must invoke before EnhancedRestTemplateInterceptor
			if (loadBalancerInterceptor != null) {
				int addIndex = list.size();
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i) instanceof EnhancedRestTemplateInterceptor) {
						addIndex = i;
					}
				}
				list.add(addIndex, loadBalancerInterceptor);
			}
			restTemplate.setInterceptors(list);
		};
	}

}
