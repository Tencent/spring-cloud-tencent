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

package com.tencent.cloud.polaris.router;

import java.util.ArrayList;
import java.util.List;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.polaris.api.pojo.DefaultInstance;
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

import org.springframework.cloud.client.ServiceInstance;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test for ${@link RouterUtils}.
 *
 * @author lepdou 2022-07-04
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterUtilsTest {

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
		ServiceInstances serviceInstances = RouterUtils.transferServersToServiceInstances(Flux.empty());
		Assert.assertNotNull(serviceInstances.getInstances());
		Assert.assertEquals(0, serviceInstances.getInstances().size());
	}

	@Test
	public void testTransferNotEmptyInstances() {
		int instanceSize = 100;
		int weight = 50;

		List<ServiceInstance> instances = new ArrayList<>();
		for (int i = 0; i < instanceSize; i++) {
			DefaultInstance instance = new DefaultInstance();
			instance.setService(testNamespaceAndService);
			instance.setId("ins" + i);
			instance.setPort(8080);
			instance.setHost("127.0.0." + i);
			instance.setWeight(weight);
			instances.add(new PolarisServiceInstance(instance));
		}

		ServiceInstances serviceInstances = RouterUtils.transferServersToServiceInstances(Flux.just(instances));

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
			Assert.assertEquals(weight, instance.getWeight());
		}
	}
}
