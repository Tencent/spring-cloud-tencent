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

package com.tencent.cloud.plugin.discovery.adapter.config;

import com.tencent.cloud.plugin.discovery.adapter.transformer.NacosInstanceTransformer;
import com.tencent.cloud.polaris.router.config.LoadBalancerConfiguration;
import com.tencent.cloud.rpc.enhancement.transformer.InstanceTransformer;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NacosDiscoveryAdapterAutoConfiguration.
 *
 * @author sean yu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@AutoConfigureBefore(LoadBalancerConfiguration.class)
public class NacosDiscoveryAdapterAutoConfiguration {


	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "com.alibaba.cloud.nacos.NacosServiceInstance")
	public InstanceTransformer instanceTransformer() {
		return new NacosInstanceTransformer();
	}

}
