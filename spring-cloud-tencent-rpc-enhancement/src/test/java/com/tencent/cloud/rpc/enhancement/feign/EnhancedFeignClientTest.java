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

package com.tencent.cloud.rpc.enhancement.feign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.rpc.enhancement.plugin.DefaultEnhancedPluginRunner;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link EnhancedFeignClient}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EnhancedFeignClientTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp", "spring.cloud.gateway.enabled=false"})
public class EnhancedFeignClientTest {

	@Autowired
	private PolarisSDKContextManager polarisSDKContextManager;

	@Test
	public void testConstructor() {
		try {
			new EnhancedFeignClient(null, null);
			fail("NullPointerException should be thrown.");
		}
		catch (Throwable e) {
			assertThat(e).isInstanceOf(NullPointerException.class);
			assertThat(e.getMessage()).isEqualTo("target");
		}

		try {
			new EnhancedFeignClient(mock(Client.class), null);
		}
		catch (Throwable e) {
			fail("Exception encountered.", e);
		}

		List<EnhancedPlugin> enhancedPlugins = getMockEnhancedFeignPlugins();
		try {
			new EnhancedFeignClient(mock(Client.class),
					new DefaultEnhancedPluginRunner(enhancedPlugins, null, polarisSDKContextManager.getSDKContext()));
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
				return Response.builder().request(request).status(502).build();
			}
			throw new IOException("Mock exception.");
		}).when(delegate).execute(any(Request.class), nullable(Request.Options.class));

		// mock target
		Target<Object> target = Target.EmptyTarget.create(Object.class);

		// mock RequestTemplate.class
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.feignTarget(target);

		EnhancedFeignClient polarisFeignClient = new EnhancedFeignClient(delegate,
				new DefaultEnhancedPluginRunner(getMockEnhancedFeignPlugins(), null, polarisSDKContextManager.getSDKContext()));

		// 200
		Response response = polarisFeignClient.execute(Request.create(Request.HttpMethod.GET, "http://localhost:8080/test",
				Collections.emptyMap(), null, requestTemplate), null);
		assertThat(response.status()).isEqualTo(200);

		// 502
		response = polarisFeignClient.execute(Request.create(Request.HttpMethod.POST, "http://localhost:8080/test",
				Collections.emptyMap(), null, requestTemplate), null);
		assertThat(response.status()).isEqualTo(502);

		// Exception
		try {
			polarisFeignClient.execute(Request.create(Request.HttpMethod.DELETE, "http://localhost:8080/test",
					Collections.emptyMap(), null, requestTemplate), null);
			fail("IOException should be thrown.");
		}
		catch (Throwable t) {
			assertThat(t).isInstanceOf(IOException.class);
			assertThat(t.getMessage()).isEqualTo("Mock exception.");
		}
	}

	private List<EnhancedPlugin> getMockEnhancedFeignPlugins() {
		List<EnhancedPlugin> enhancedPlugins = new ArrayList<>();

		enhancedPlugins.add(new EnhancedPlugin() {
			@Override
			public EnhancedPluginType getType() {
				return EnhancedPluginType.Client.PRE;
			}

			@Override
			public void run(EnhancedPluginContext context) {

			}

			@Override
			public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {

			}

			@Override
			public int getOrder() {
				return 0;
			}
		});

		enhancedPlugins.add(new EnhancedPlugin() {
			@Override
			public EnhancedPluginType getType() {
				return EnhancedPluginType.Client.POST;
			}

			@Override
			public void run(EnhancedPluginContext context) {

			}

			@Override
			public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {

			}

			@Override
			public int getOrder() {
				return 0;
			}
		});

		enhancedPlugins.add(new EnhancedPlugin() {
			@Override
			public EnhancedPluginType getType() {
				return EnhancedPluginType.Client.EXCEPTION;
			}

			@Override
			public void run(EnhancedPluginContext context) {

			}

			@Override
			public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {

			}

			@Override
			public int getOrder() {
				return 0;
			}
		});

		enhancedPlugins.add(new EnhancedPlugin() {
			@Override
			public EnhancedPluginType getType() {
				return EnhancedPluginType.Client.FINALLY;
			}

			@Override
			public void run(EnhancedPluginContext context) {

			}

			@Override
			public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {

			}

			@Override
			public int getOrder() {
				return 0;
			}
		});

		return enhancedPlugins;

	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
