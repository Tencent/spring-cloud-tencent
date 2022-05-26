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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.io.IOException;

import com.google.common.collect.Maps;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisFeignClient}.
 *
 * @author Haotian Zhang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PolarisFeignClientTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"})
public class PolarisFeignClientTest {

	@Test
	public void testConstructor() {
		try {
			new PolarisFeignClient(null, null);
			fail("NullPointerException should be thrown.");
		}
		catch (Throwable e) {
			assertThat(e).isInstanceOf(NullPointerException.class);
			assertThat(e.getMessage()).isEqualTo("target");
		}

		try {
			new PolarisFeignClient(mock(Client.class), null);
			fail("NullPointerException should be thrown.");
		}
		catch (Throwable e) {
			assertThat(e).isInstanceOf(NullPointerException.class);
			assertThat(e.getMessage()).isEqualTo("CircuitBreakAPI");
		}

		try {
			assertThat(new PolarisFeignClient(mock(Client.class), mock(ConsumerAPI.class))).isInstanceOf(PolarisFeignClient.class);
		}
		catch (Throwable e) {
			fail("Exception encountered.", e);
		}
	}

	@Test
	public void testExecute() throws IOException {
		// mock Client.class
		Client delegate = mock(Client.class);
		doAnswer(invocation -> {
			Request request = invocation.getArgument(0);
			if (request.httpMethod().equals(Request.HttpMethod.GET)) {
				return Response.builder().request(request).status(200).build();
			}
			else if (request.httpMethod().equals(Request.HttpMethod.POST)) {
				return Response.builder().request(request).status(500).build();
			}
			throw new IOException("Mock exception.");
		}).when(delegate).execute(any(Request.class), nullable(Request.Options.class));

		// mock ConsumerAPI.class
		ConsumerAPI consumerAPI = mock(ConsumerAPI.class);
		doNothing().when(consumerAPI).updateServiceCallResult(any(ServiceCallResult.class));

		// mock target
		Target<Object> target = Target.EmptyTarget.create(Object.class);

		// mock RequestTemplate.class
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.feignTarget(target);

		PolarisFeignClient polarisFeignClient = new PolarisFeignClient(delegate, consumerAPI);

		// 200
		Response response = polarisFeignClient.execute(Request.create(Request.HttpMethod.GET, "http://localhost:8080/test",
				Maps.newHashMap(), null, requestTemplate), null);
		assertThat(response.status()).isEqualTo(200);

		// 200
		response = polarisFeignClient.execute(Request.create(Request.HttpMethod.POST, "http://localhost:8080/test",
				Maps.newHashMap(), null, requestTemplate), null);
		assertThat(response.status()).isEqualTo(500);

		// Exception
		try {
			polarisFeignClient.execute(Request.create(Request.HttpMethod.DELETE, "http://localhost:8080/test",
					Maps.newHashMap(), null, requestTemplate), null);
			fail("IOException should be thrown.");
		}
		catch (Throwable t) {
			assertThat(t).isInstanceOf(IOException.class);
			assertThat(t.getMessage()).isEqualTo("Mock exception.");
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
