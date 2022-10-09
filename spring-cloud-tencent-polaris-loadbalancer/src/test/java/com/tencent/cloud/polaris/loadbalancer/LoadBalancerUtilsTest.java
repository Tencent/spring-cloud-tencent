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

package com.tencent.cloud.polaris.loadbalancer;

import java.util.LinkedList;
import java.util.List;

import com.netflix.loadbalancer.Server;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test for {@link LoadBalancerUtilsTest} .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-06-21
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LoadBalancerUtilsTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace = testNamespace",
				"spring.cloud.polaris.loadbalancer.enabled = false",
				"spring.application.name = testApp",
				"spring.main.web-application-type = none"})
public class LoadBalancerUtilsTest {

	private static final String TEST_NAMESPACE = "testNamespace";
	private static final String TEST_SERVICE = "testService";

	@Test
	public void testTransferServersToServiceInstances() {
		ServiceInstances serviceInstances = LoadBalancerUtils.transferServersToServiceInstances(assembleServers());
		Assertions.assertThat(serviceInstances).isInstanceOf(DefaultServiceInstances.class);
		Assertions.assertThat(serviceInstances.getInstances().size()).isEqualTo(4);
		ServiceKey actual = serviceInstances.getServiceKey();
		Assertions.assertThat(actual.getNamespace()).isEqualTo(TEST_NAMESPACE);
		Assertions.assertThat(actual.getService()).isEqualTo(TEST_SERVICE);
	}

	private ServiceInstances assembleServiceInstances() {
		ServiceKey serviceKey = new ServiceKey(TEST_NAMESPACE, TEST_SERVICE);
		List<Instance> instances = new LinkedList<>();
		DefaultInstance instance = new DefaultInstance();
		instance.setService(TEST_SERVICE);
		instances.add(instance);
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());
		instances.add(new DefaultInstance());

		return new DefaultServiceInstances(serviceKey, instances);
	}

	private List<Server> assembleServers() {
		ServiceInstances serviceInstances = assembleServiceInstances();
		List<Server> servers = new LinkedList<>();
		DefaultInstance instance = new DefaultInstance();
		instance.setService(TEST_SERVICE);
		servers.add(new PolarisServer(serviceInstances, instance));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		servers.add(new PolarisServer(serviceInstances, new DefaultInstance()));
		return servers;
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
