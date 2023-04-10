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

package com.tencent.cloud.rpc.enhancement.webclient;

import java.net.URI;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.mockito.ArgumentMatchers.anyString;

public class EnhancedWebClientReporterTest {

	private static final String URI_TEMPLATE_ATTRIBUTE = EnhancedWebClientReporterTest.class.getName() + ".uriTemplate";

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testInstrumentResponse() {
		ClientResponse response = Mockito.mock(ClientResponse.class);
		ClientResponse.Headers headers = Mockito.mock(ClientResponse.Headers.class);
		Mockito.doReturn(headers).when(response).headers();
		Mockito.doReturn(new HttpHeaders()).when(headers).asHttpHeaders();
		Mono<ClientResponse> responseMono = Mono.just(response);
		ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "https://example.org/projects/{project}")
				.build();

		ConsumerAPI consumerAPI = Mockito.mock(ConsumerAPI.class);
		Mockito.doAnswer(invocationOnMock -> {
			ServiceCallResult result = invocationOnMock.getArgument(0, ServiceCallResult.class);
			Assertions.assertTrue(result.getDelay() > 0);
			return null;
		}).when(consumerAPI)
				.updateServiceCallResult(Mockito.any(ServiceCallResult.class));

		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		properties.setEnabled(true);
		properties.getStatuses().clear();
		properties.getSeries().clear();
//		EnhancedWebClientReporter reporter = new EnhancedWebClientReporter(properties, null, consumerAPI);
//
//		reporter.instrumentResponse(request, responseMono)
//				.contextWrite(context -> context.put(METRICS_WEBCLIENT_START_TIME, System.currentTimeMillis()))
//				.subscribe();
	}

}
