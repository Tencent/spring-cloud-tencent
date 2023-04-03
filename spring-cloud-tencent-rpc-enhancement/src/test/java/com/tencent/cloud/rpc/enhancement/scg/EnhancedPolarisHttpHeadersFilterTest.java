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

package com.tencent.cloud.rpc.enhancement.scg;

import java.util.Collections;

import com.tencent.cloud.common.constant.HeaderConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;

public class EnhancedPolarisHttpHeadersFilterTest {

	@Test
	public void testFilter() {
		EnhancedPolarisHttpHeadersFilter filter = new EnhancedPolarisHttpHeadersFilter();

		ServiceInstance instance = Mockito.mock(ServiceInstance.class);
		Mockito.doReturn("mock_service").when(instance).getServiceId();
		Mockito.doReturn("127.0.0.1").when(instance).getHost();
		Mockito.doReturn(8080).when(instance).getPort();
		DefaultResponse response = new DefaultResponse(instance);

		ServerWebExchange exchange = Mockito.mock(ServerWebExchange.class);
		Mockito.doReturn(response).when(exchange).getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);

		HttpHeaders input = new HttpHeaders();
		HttpHeaders headers = filter.filter(input, exchange);

		Assertions.assertEquals(Collections.singletonList("mock_service"), headers.get(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID));
		Assertions.assertEquals(Collections.singletonList("127.0.0.1"), headers.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST));
		Assertions.assertEquals(Collections.singletonList("8080"), headers.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT));
	}

}