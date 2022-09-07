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

import com.tencent.cloud.common.pojo.PolarisServiceInstance;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisLoadBalancer}.
 *
 * @author rod.xu
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisLoadBalancerTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static Instance testInstance;
	@Mock
	private RouterAPI routerAPI;
	@Mock
	private ObjectProvider<ServiceInstanceListSupplier> supplierObjectProvider;
	@Mock
	private PolarisLoadBalancerProperties loadBalancerProperties;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");

		testInstance = Instance.createDefaultInstance("instance-id", LOCAL_NAMESPACE,
				LOCAL_SERVICE, "host", 8090);
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
	}

	@Test
	public void chooseNormalLogicTest_thenReturnAvailablePolarisInstance() {

		Request request = Mockito.mock(Request.class);
		List<ServiceInstance> mockInstanceList = new ArrayList<>();
		mockInstanceList.add(new PolarisServiceInstance(testInstance));

		ServiceInstanceListSupplier serviceInstanceListSupplier = Mockito.mock(ServiceInstanceListSupplier.class);
		when(serviceInstanceListSupplier.get(request)).thenReturn(Flux.just(mockInstanceList));

		when(supplierObjectProvider.getIfAvailable(any())).thenReturn(serviceInstanceListSupplier);
		when(loadBalancerProperties.getEnabled()).thenReturn(true);

		ProcessLoadBalanceResponse mockLbRes = new ProcessLoadBalanceResponse(testInstance);
		when(routerAPI.processLoadBalance(any())).thenReturn(mockLbRes);

		// request construct and execute invoke
		PolarisLoadBalancer polarisLoadBalancer = new PolarisLoadBalancer(LOCAL_SERVICE, supplierObjectProvider,
				loadBalancerProperties, routerAPI);
		Mono<Response<ServiceInstance>> responseMono = polarisLoadBalancer.choose(request);
		ServiceInstance serviceInstance = responseMono.block().getServer();

		// verify method has invoked
		verify(loadBalancerProperties).getEnabled();
		verify(supplierObjectProvider).getIfAvailable(any());

		//result assert
		Assertions.assertThat(serviceInstance).isNotNull();
		Assertions.assertThat(serviceInstance instanceof PolarisServiceInstance).isTrue();

		PolarisServiceInstance polarisServiceInstance = (PolarisServiceInstance) serviceInstance;

		Assertions.assertThat(polarisServiceInstance.getPolarisInstance().getId()).isEqualTo("instance-id");
		Assertions.assertThat(polarisServiceInstance.getPolarisInstance().getNamespace()).isEqualTo(LOCAL_NAMESPACE);
		Assertions.assertThat(polarisServiceInstance.getPolarisInstance().getService()).isEqualTo(LOCAL_SERVICE);
		Assertions.assertThat(polarisServiceInstance.getPolarisInstance().getHost()).isEqualTo("host");
		Assertions.assertThat(polarisServiceInstance.getPolarisInstance().getPort()).isEqualTo(8090);
	}

}
