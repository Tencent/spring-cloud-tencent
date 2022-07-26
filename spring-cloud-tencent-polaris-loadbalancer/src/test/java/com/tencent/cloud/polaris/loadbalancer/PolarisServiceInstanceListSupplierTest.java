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

import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test for {@link PolarisServiceInstanceListSupplier}
 *
 * @author rod.xu
 * @date 2022/7/21 5:45 下午
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisServiceInstanceListSupplierTest {

	@Mock
	private ServiceInstanceListSupplier serviceInstanceListSupplier;

	@Test
	public void chooseInstancesTest() {
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn("test-unit");

			PolarisServiceInstanceListSupplier instanceListSupplier =
					new PolarisServiceInstanceListSupplier(serviceInstanceListSupplier);

			List<ServiceInstance> allServers = new ArrayList<>();
			ServiceInstance instance1 = new DefaultServiceInstance("unit-test-instanceId-01",
					"unit-test-serviceId", "unit-test-host-01", 8090, false);
			ServiceInstance instance2 = new DefaultServiceInstance("unit-test-instanceId-02",
					"unit-test-serviceId", "unit-test-host-02", 8090, false);

			allServers.add(instance1);
			allServers.add(instance2);

			List<ServiceInstance> polarisInstanceList = instanceListSupplier.chooseInstances(allServers);
			Assert.assertNotNull(polarisInstanceList);
			Assert.assertEquals(polarisInstanceList.size(), allServers.size());
			for (ServiceInstance serviceInstance : polarisInstanceList) {
				Assert.assertTrue("", serviceInstance instanceof PolarisServiceInstance);
				PolarisServiceInstance polarisServiceInstance = (PolarisServiceInstance) serviceInstance;

				Assert.assertFalse(polarisServiceInstance.isSecure());
				Assert.assertEquals("unit-test-serviceId", polarisServiceInstance.getPolarisInstance().getService());
				Assert.assertEquals(8090, polarisServiceInstance.getPolarisInstance().getPort());
				Assert.assertEquals(LOCAL_NAMESPACE, polarisServiceInstance.getPolarisInstance().getNamespace());

				Assert.assertTrue("instance id assert",
						polarisServiceInstance.getPolarisInstance().getId().startsWith("unit-test-instanceId"));
				Assert.assertTrue("host assert",
						polarisServiceInstance.getPolarisInstance().getHost().startsWith("unit-test-host"));
			}
		}
	}
}
