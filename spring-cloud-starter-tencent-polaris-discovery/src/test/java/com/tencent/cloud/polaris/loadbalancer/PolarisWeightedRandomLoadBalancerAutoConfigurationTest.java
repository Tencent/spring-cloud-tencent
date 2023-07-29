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

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PolarisLoadBalancerAutoConfiguration}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class PolarisWeightedRandomLoadBalancerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withConfiguration(AutoConfigurations.of(
					PolarisRibbonTest.class,
					PolarisLoadBalancerAutoConfiguration.class,
					PolarisContextAutoConfiguration.class));

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testDefaultInitialization() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(RestTemplate.class);
			assertThatThrownBy(() -> {
				context.getBean(RestTemplate.class).getForEntity("http://wrong.url", String.class);
			}).isInstanceOf(Exception.class);
		});
	}

	@Test
	public void testRandomInitialization() {
		this.contextRunner.withPropertyValues("spring.cloud.polaris.loadbalancer.strategy=random").run(context -> {
			assertThat(context).hasSingleBean(RestTemplate.class);
			assertThatThrownBy(() -> {
				context.getBean(RestTemplate.class).getForEntity("http://wrong.url", String.class);
			}).isInstanceOf(Exception.class);
		});
	}

	@Test
	public void testPolarisWeightedInitialization() {
		this.contextRunner.withPropertyValues("spring.cloud.polaris.loadbalancer.strategy=polarisWeightedRandom")
				.run(context -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					assertThatThrownBy(() -> {
						context.getBean(RestTemplate.class).getForEntity("http://wrong.url", String.class);
					}).isInstanceOf(Exception.class);
				});
	}

	@Test
	public void testPolarisWeightedRoundRobinInitialization() {
		this.contextRunner.withPropertyValues("spring.cloud.polaris.loadbalancer.strategy=polarisWeightedRoundRobin")
				.run(context -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					assertThatThrownBy(() -> {
						context.getBean(RestTemplate.class).getForEntity("http://wrong.url", String.class);
					}).isInstanceOf(Exception.class);
				});
	}


	@Test
	public void testPolarisRingHashInitialization() {
		this.contextRunner
				.withPropertyValues("spring.cloud.polaris.loadbalancer.strategy=polarisRingHash").run(context -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					assertThatThrownBy(() -> {
						context.getBean(RestTemplate.class).getForEntity("http://wrong.url", String.class);
					}).isInstanceOf(Exception.class);
				});
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisRibbonTest {
		@Bean
		@LoadBalanced
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}
	}
}
