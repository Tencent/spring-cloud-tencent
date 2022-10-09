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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.DummyPing;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.netflix.ribbon.StaticServerList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisLoadBalancer}.
 *
 * @author lapple.lei 2022-06-28
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisLoadBalancerTest {
	private static final String CLIENT_NAME = "polaris-test-server";
	private static final String NS = "testNamespace";
	private static final String[] HOST_LIST = new String[] {
			"127.0.0.1",
			"127.0.0.2",
			"127.0.0.3",
			"127.0.0.4",
			"127.0.0.5",
	};

	@Mock
	private RouterAPI routerAPI;
	@Mock
	private ConsumerAPI consumerAPI;

	@Test
	public void testPolarisLoadBalancer() {
		//mock consumerAPI
		when(consumerAPI.getHealthyInstances(any())).thenReturn(this.assembleInstanceResp());

		//mock routerAPI for rule
		when(routerAPI.processLoadBalance(any())).thenReturn(assembleProcessLoadBalanceResp());
		PolarisWeightedRule rule = new PolarisWeightedRule(routerAPI);

		// clientConfig
		IClientConfig config = new DefaultClientConfigImpl();
		config.loadProperties(CLIENT_NAME);

		//mock for MetadataContext
		try (MockedStatic<ApplicationContextAwareUtils> mockedCtxUtils = Mockito
				.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedCtxUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
					.thenReturn(NS);
			mockedCtxUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
					.thenReturn("TestServer");

			PolarisLoadBalancerProperties properties = new PolarisLoadBalancerProperties();
			ServerList<Server> emptyServerList = new StaticServerList<>();

			PolarisLoadBalancer balancer = new PolarisLoadBalancer(config, rule, new DummyPing(), emptyServerList,
					consumerAPI, properties);

			String host = balancer.choose(null);

			Assertions.assertThat(host).isNotNull();
			Assertions.assertThat(host).isEqualTo("127.0.0.1:8080");
		}
	}

	@Test
	public void testExtendDiscoveryServiceInstance() {
		//mock routerAPI for rule
		when(routerAPI.processLoadBalance(any())).thenReturn(assembleProcessLoadBalanceResp());
		PolarisWeightedRule rule = new PolarisWeightedRule(routerAPI);

		// clientConfig
		IClientConfig config = new DefaultClientConfigImpl();
		config.loadProperties(CLIENT_NAME);

		//mock for MetadataContext
		try (MockedStatic<ApplicationContextAwareUtils> mockedCtxUtils = Mockito
				.mockStatic(ApplicationContextAwareUtils.class)) {

			mockedCtxUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace"))
					.thenReturn(NS);
			mockedCtxUtils.when(() -> ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service"))
					.thenReturn("TestServer");

			PolarisLoadBalancerProperties properties = new PolarisLoadBalancerProperties();
			properties.setDiscoveryType("TEST");
			ServerList<Server> staticServerList = assembleServerList();

			PolarisLoadBalancer balancer = new PolarisLoadBalancer(config, rule, new DummyPing(), staticServerList,
					consumerAPI, properties);

			String host = balancer.choose(null);
			Assertions.assertThat(host).isEqualTo("127.0.0.1:8080");
		}
	}

	private ServerList<Server> assembleServerList() {
		return new StaticServerList<>(Stream.of(HOST_LIST).map(this::convertServer).toArray(Server[]::new));
	}

	private ProcessLoadBalanceResponse assembleProcessLoadBalanceResp() {
		ServiceInstances serviceInstances = assembleServiceInstances();
		return new ProcessLoadBalanceResponse(serviceInstances.getInstances().get(0));
	}

	private InstancesResponse assembleInstanceResp() {
		return new InstancesResponse(assembleServiceInstances());
	}

	private ServiceInstances assembleServiceInstances() {
		ServiceKey serviceKey = new ServiceKey(NS, CLIENT_NAME);

		List<Instance> instances = Stream.of(HOST_LIST).map(this::convertInstance).collect(Collectors.toList());
		return new DefaultServiceInstances(serviceKey, instances);
	}

	private Instance convertInstance(String host) {
		DefaultInstance instance = new DefaultInstance();
		instance.setHost(host);
		instance.setPort(8080);
		return instance;
	}

	private Server convertServer(String host) {
		return new Server("http", host, 8080);
	}
}
