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

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test for ${@link LoadBalancerUtils}.
 *
 * @author lepdou 2022-07-04
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadBalancerUtilsTest {

	private static final String testNamespaceAndService = "testNamespaceAndService";
	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<MetadataContextHolder> mockedMetadataContextHolder;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn(testNamespaceAndService);

		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);
		mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class);
		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
		mockedMetadataContextHolder.close();
	}

	@Test
	public void testTransferEmptyInstances() {
		ServiceInstances serviceInstances = LoadBalancerUtils.transferServersToServiceInstances(Flux.empty());
		Assert.assertNotNull(serviceInstances.getInstances());
		Assert.assertEquals(0, serviceInstances.getInstances().size());
	}

	@Test
	public void testTransferNotEmptyInstances() {
		int instanceSize = 100;

		List<ServiceInstance> instances = new ArrayList<>();
		for (int i = 0; i < instanceSize; i++) {
			instances.add(new DefaultServiceInstance("ins" + i, testNamespaceAndService, "127.0.0." + i,
					8080, false));
		}

		ServiceInstances serviceInstances = LoadBalancerUtils.transferServersToServiceInstances(Flux.just(instances));

		Assert.assertNotNull(serviceInstances.getInstances());
		Assert.assertEquals(instanceSize, serviceInstances.getInstances().size());

		List<Instance> polarisInstances = serviceInstances.getInstances();
		for (int i = 0; i < instanceSize; i++) {
			Instance instance = polarisInstances.get(i);
			Assert.assertEquals(testNamespaceAndService, instance.getNamespace());
			Assert.assertEquals(testNamespaceAndService, instance.getService());
			Assert.assertEquals("ins" + i, instance.getId());
			Assert.assertEquals("127.0.0." + i, instance.getHost());
			Assert.assertEquals(8080, instance.getPort());
			Assert.assertEquals(100, instance.getWeight());
		}
	}
}
