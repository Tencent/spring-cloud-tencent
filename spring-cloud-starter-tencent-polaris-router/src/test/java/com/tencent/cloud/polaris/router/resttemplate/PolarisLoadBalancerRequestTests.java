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

package com.tencent.cloud.polaris.router.resttemplate;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.http.HttpRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * test for {@link PolarisLoadBalancerRequest}.
 * @author dongyinuo
 */
public class PolarisLoadBalancerRequestTests {

	@Test
	public void test() throws Exception {
		String calleeService = "calleeService";
		HttpRequest request = new RouterLabelRestTemplateInterceptorTest.MockedHttpRequest("http://" + calleeService + "/user/get");
		MockLoadBalancerRequest mockLoadBalancerRequest = new MockLoadBalancerRequest();
		PolarisLoadBalancerRequest<ServiceInstance> polarisLoadBalancerRequest = new PolarisLoadBalancerRequest<>(request, mockLoadBalancerRequest);

		DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
		serviceInstance.setServiceId(calleeService);
		ServiceInstance apply = polarisLoadBalancerRequest.apply(serviceInstance);
		assertThat(apply.getServiceId()).isEqualTo(calleeService);
		assertThat(polarisLoadBalancerRequest.getRequest()).isEqualTo(request);
		assertThat(polarisLoadBalancerRequest.getDelegate()).isEqualTo(mockLoadBalancerRequest);
	}

	static class MockLoadBalancerRequest implements LoadBalancerRequest {

		@Override
		public Object apply(ServiceInstance instance) throws Exception {
			return instance;
		}
	}

}
