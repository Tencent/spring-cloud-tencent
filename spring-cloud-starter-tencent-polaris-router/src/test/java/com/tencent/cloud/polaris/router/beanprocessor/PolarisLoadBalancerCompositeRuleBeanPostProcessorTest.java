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

package com.tencent.cloud.polaris.router.beanprocessor;

import java.util.List;

import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.PolarisLoadBalancerCompositeRule;
import com.tencent.cloud.polaris.router.config.RibbonConfiguration;
import com.tencent.cloud.polaris.router.config.properties.PolarisNearByRouterProperties;
import com.tencent.cloud.polaris.router.interceptor.NearbyRouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.client.api.DefaultRouterAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLoadBalancerCompositeRuleBeanPostProcessor}.
 *
 * @author derekyi 2022-08-01
 */
public class PolarisLoadBalancerCompositeRuleBeanPostProcessorTest {

	private static final String SERVICE_1 = "service1";

	private static final String SERVICE_2 = "service2";

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void test1() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PolarisContextAutoConfiguration.class,
						RibbonDefaultConfig.class,
						PolarisRibbonTest.class,
						RibbonAutoConfiguration.class,
						RouterConfiguration.class));
		contextRunner.run(context -> {
			SpringClientFactory springClientFactory = context.getBean(SpringClientFactory.class);

			IRule rule = springClientFactory.getInstance(SERVICE_1, IRule.class);
			assertThat(rule).isInstanceOf(PolarisLoadBalancerCompositeRule.class);
			List<RouterRequestInterceptor> requestInterceptors = (List<RouterRequestInterceptor>) ReflectionUtils.getFieldValue(rule, "requestInterceptors");
			assertThat(requestInterceptors).isNotEmpty();
			AbstractLoadBalancerRule delegateRule = ((PolarisLoadBalancerCompositeRule) rule).getDelegateRule();
			//ZoneAvoidanceRule default
			assertThat(delegateRule).isInstanceOf(ZoneAvoidanceRule.class);
		});
	}

	@Test
	public void test2() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class,
						RibbonDefaultConfig.class, PolarisRibbonTest.class, RibbonAutoConfiguration.class))
				.withPropertyValues("spring.cloud.polaris.loadbalancer.strategy = random");
		contextRunner.run(context -> {
			SpringClientFactory springClientFactory = context.getBean(SpringClientFactory.class);

			IRule rule = springClientFactory.getInstance(SERVICE_1, IRule.class);
			assertThat(rule).isInstanceOf(PolarisLoadBalancerCompositeRule.class);
			AbstractLoadBalancerRule delegateRule = ((PolarisLoadBalancerCompositeRule) rule).getDelegateRule();
			//spring.cloud.polaris.loadbalancer.strategy = random
			assertThat(delegateRule).isInstanceOf(RandomRule.class);
		});
	}

	@Test
	public void test3() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class,
						RibbonDefaultConfig.class, PolarisRibbonTest.class, RibbonAutoConfiguration.class))
				.withPropertyValues("service1.ribbon.NFLoadBalancerRuleClassName=com.netflix.loadbalancer.RoundRobinRule");
		contextRunner.run(context -> {
			SpringClientFactory springClientFactory = context.getBean(SpringClientFactory.class);

			IRule rule1 = springClientFactory.getInstance(SERVICE_1, IRule.class);
			assertThat(rule1).isInstanceOf(PolarisLoadBalancerCompositeRule.class);
			AbstractLoadBalancerRule delegateRule1 = ((PolarisLoadBalancerCompositeRule) rule1).getDelegateRule();
			//service1.ribbon.NFLoadBalancerRuleClassName=com.netflix.loadbalancer.RoundRobinRule
			assertThat(delegateRule1).isInstanceOf(RoundRobinRule.class);

			IRule rule2 = springClientFactory.getInstance(SERVICE_2, IRule.class);
			assertThat(rule2).isInstanceOf(PolarisLoadBalancerCompositeRule.class);

			AbstractLoadBalancerRule delegateRule2 = ((PolarisLoadBalancerCompositeRule) rule2).getDelegateRule();
			//ZoneAvoidanceRule default
			assertThat(delegateRule2).isInstanceOf(ZoneAvoidanceRule.class);
		});
	}

	@Test
	public void test4() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(PolarisContextAutoConfiguration.class,
						CustomRibbonConfig.class, PolarisRibbonTest.class, RibbonAutoConfiguration.class));
		contextRunner.run(context -> {
			SpringClientFactory springClientFactory = context.getBean(SpringClientFactory.class);

			IRule rule1 = springClientFactory.getInstance(SERVICE_1, IRule.class);
			assertThat(rule1).isInstanceOf(PolarisLoadBalancerCompositeRule.class);
			AbstractLoadBalancerRule delegateRule1 = ((PolarisLoadBalancerCompositeRule) rule1).getDelegateRule();
			//RibbonConfigForService1#loadBalancerRule returns BestAvailableRule
			assertThat(delegateRule1).isInstanceOf(BestAvailableRule.class);

			IRule rule2 = springClientFactory.getInstance(SERVICE_2, IRule.class);
			assertThat(rule2).isInstanceOf(PolarisLoadBalancerCompositeRule.class);
			AbstractLoadBalancerRule delegateRule2 = ((PolarisLoadBalancerCompositeRule) rule2).getDelegateRule();
			//ZoneAvoidanceRule default
			assertThat(delegateRule2).isInstanceOf(ZoneAvoidanceRule.class);
		});
	}

	@Configuration
	@RibbonClients(defaultConfiguration = {RibbonConfiguration.class})
	static class RibbonDefaultConfig {
	}

	@Configuration
	@RibbonClients(value = {@RibbonClient(name = SERVICE_1, configuration = RibbonConfigForService1.class)}, defaultConfiguration = RibbonConfiguration.class)
	static class CustomRibbonConfig {
	}

	static class RibbonConfigForService1 {

		@Bean
		public IRule loadBalancerRule() {
			return new BestAvailableRule();
		}
	}


	@Configuration
	@EnableConfigurationProperties(PolarisLoadBalancerProperties.class)
	static class PolarisRibbonTest {

		@Bean
		public SDKContext sdkContext() {
			return SDKContext.initContext();
		}

		@Bean
		public RouterAPI routerAPI(SDKContext sdkContext) {
			return new DefaultRouterAPI(sdkContext);
		}
	}

	@Configuration
	@EnableConfigurationProperties(PolarisNearByRouterProperties.class)
	static class RouterConfiguration {
		@Bean
		@ConditionalOnProperty(value = "spring.cloud.polaris.router.nearby-router.enabled", matchIfMissing = true)
		public NearbyRouterRequestInterceptor nearbyRouterRequestInterceptor(PolarisNearByRouterProperties polarisNearByRouterProperties) {
			return new NearbyRouterRequestInterceptor(polarisNearByRouterProperties);
		}
	}
}
