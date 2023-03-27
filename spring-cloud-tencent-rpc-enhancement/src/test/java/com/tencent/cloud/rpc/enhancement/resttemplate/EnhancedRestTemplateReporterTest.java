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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EnhancedRestTemplateReporter}.
 *
 * @author lepdou 2022-09-06
 */
@ExtendWith(MockitoExtension.class)
public class EnhancedRestTemplateReporterTest {

	private static MockedStatic<MetadataContextHolder> mockedMetadataContextHolder;
	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private ConsumerAPI consumerAPI;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private ResponseErrorHandler delegate;
	@InjectMocks
	private EnhancedRestTemplateReporter enhancedRestTemplateReporter;

	@InjectMocks
	private EnhancedRestTemplateReporter enhancedRestTemplateReporter2;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("caller");
		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

		// mock transitive metadata
		Map<String, String> loadBalancerContext = new HashMap<>();
		loadBalancerContext.put("host", "1.1.1.1");
		loadBalancerContext.put("port", "8080");
		loadBalancerContext.put("startMillis", String.valueOf(System.currentTimeMillis()));
		when(metadataContext.getLoadbalancerMetadata()).thenReturn(loadBalancerContext);

		mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class);
		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
		mockedMetadataContextHolder.close();
	}

	@BeforeEach
	void setUp() {
		enhancedRestTemplateReporter.setDelegateHandler(delegate);
	}

	@Test
	public void testSetApplicationContext() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);

		// test no ResponseErrorHandler
		when(applicationContext.getBeanNamesForType(any(Class.class)))
				.thenReturn(new String[] {"enhancedRestTemplateReporter"});
		enhancedRestTemplateReporter2.setApplicationContext(applicationContext);
		assertThat(enhancedRestTemplateReporter2.getDelegateHandler()).isInstanceOf(DefaultResponseErrorHandler.class);

		// test one other ResponseErrorHandler
		when(applicationContext.getBeanNamesForType(any(Class.class)))
				.thenReturn(new String[] {"enhancedRestTemplateReporter", "mockedResponseErrorHandler"});
		when(applicationContext.getBean(anyString())).thenReturn(mock(MockedResponseErrorHandler.class));
		enhancedRestTemplateReporter2.setApplicationContext(applicationContext);
		assertThat(enhancedRestTemplateReporter2.getDelegateHandler()).isInstanceOf(MockedResponseErrorHandler.class);
	}

	@Test
	public void testHasError() throws IOException {
		when(delegate.hasError(any())).thenReturn(true);

		MockedClientHttpResponse response = new MockedClientHttpResponse();
		assertThat(enhancedRestTemplateReporter.hasError(response)).isTrue();

		String realHasError = response.getHeaders().getFirst(EnhancedRestTemplateReporter.HEADER_HAS_ERROR);
		assertThat(realHasError).isEqualTo("true");
	}

	@Test
	public void testHandleHasError() throws IOException {
		when(reporterProperties.isEnabled()).thenReturn(true);
		when(delegate.hasError(any())).thenReturn(true);

		MockedClientHttpResponse response = new MockedClientHttpResponse();
		enhancedRestTemplateReporter.hasError(response);

		URI uri = mock(URI.class);
		enhancedRestTemplateReporter.handleError(uri, HttpMethod.GET, response);

		verify(consumerAPI, times(1)).updateServiceCallResult(any());
		verify(delegate).handleError(uri, HttpMethod.GET, response);
	}

	@Test
	public void testHandleHasNotError() throws IOException {
		when(reporterProperties.isEnabled()).thenReturn(true);
		when(delegate.hasError(any())).thenReturn(false);

		MockedClientHttpResponse response = new MockedClientHttpResponse();
		enhancedRestTemplateReporter.hasError(response);

		URI uri = mock(URI.class);
		enhancedRestTemplateReporter.handleError(uri, HttpMethod.GET, response);

		verify(consumerAPI, times(1)).updateServiceCallResult(any());
		verify(delegate, times(0)).handleError(uri, HttpMethod.GET, response);
	}

	@Test
	public void testReportSwitchOff() throws IOException {
		when(reporterProperties.isEnabled()).thenReturn(false);
		when(delegate.hasError(any())).thenReturn(true);

		MockedClientHttpResponse response = new MockedClientHttpResponse();
		enhancedRestTemplateReporter.hasError(response);

		URI uri = mock(URI.class);
		enhancedRestTemplateReporter.handleError(uri, HttpMethod.GET, response);

		verify(consumerAPI, times(0)).updateServiceCallResult(any());
		verify(delegate).handleError(uri, HttpMethod.GET, response);
	}

	static class MockedClientHttpResponse extends AbstractClientHttpResponse {

		private final HttpHeaders headers;

		MockedClientHttpResponse() {
			this.headers = new HttpHeaders();
		}

		@Override
		public int getRawStatusCode() {
			return 0;
		}

		@Override
		public String getStatusText() {
			return null;
		}

		@Override
		public void close() {

		}

		@Override
		public InputStream getBody() throws IOException {
			return null;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return HttpStatus.OK;
		}
	}

	private static class MockedResponseErrorHandler extends DefaultResponseErrorHandler {

		@Override
		public void handleError(@NonNull ClientHttpResponse response) {
		}

	}
}
