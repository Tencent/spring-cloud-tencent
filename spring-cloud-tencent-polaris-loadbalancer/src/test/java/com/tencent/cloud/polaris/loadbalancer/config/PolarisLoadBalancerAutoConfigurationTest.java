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

package com.tencent.cloud.polaris.loadbalancer.config;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.NoOpPing;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.api.DefaultConsumerAPI;
import com.tencent.polaris.router.api.core.RouterAPI;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLoadBalancerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
public class PolarisLoadBalancerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PolarisRibbonTest.class,
					PolarisLoadBalancerAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					RibbonAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081");

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(RouterAPI.class);
			assertThat(context).hasSingleBean(PolarisLoadBalancerProperties.class);
			assertThat(hasSinglePolarisLoadBalancer(context)).isTrue();
		});
	}

	private boolean hasSinglePolarisLoadBalancer(BeanFactory beanFactory) {
		SpringClientFactory contextBean = beanFactory.getBean(SpringClientFactory.class);
		ILoadBalancer loadBalancer = contextBean.getLoadBalancer(SERVICE_PROVIDER);
		return loadBalancer instanceof PolarisLoadBalancer;
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisRibbonTest {

		@Autowired
		private SDKContext sdkContext;

		@Bean
		public IClientConfig iClientConfig() {
			DefaultClientConfigImpl config = new DefaultClientConfigImpl();
			config.setClientName(SERVICE_PROVIDER);
			return config;
		}

		@Bean
		public IRule iRule() {
			return new RandomRule();
		}

		@Bean
		public IPing iPing() {
			return new NoOpPing();
		}

		@Bean
		public ServerList<Server> serverList() {
			return new StaticServerList<>();
		}

		@Bean
		public ConsumerAPI consumerAPI() {
			return new DefaultConsumerAPI(sdkContext);
		}
	}
}
