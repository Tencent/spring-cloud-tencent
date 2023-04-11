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
import java.net.URISyntaxException;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class EnhancedWebClientReporterTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@Mock
	private RpcEnhancementReporterProperties reporterProperties;
	@Mock
	private SDKContext sdkContext;
	@Mock
	private ConsumerAPI consumerAPI;
	@Mock
	private CircuitBreakAPI circuitBreakAPI;
	@Mock
	private ClientRequest clientRequest;
	@Mock
	private ExchangeFunction exchangeFunction;
	@Mock
	private ClientResponse clientResponse;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		MetadataLocalProperties metadataLocalProperties = mock(MetadataLocalProperties.class);
		StaticMetadataManager staticMetadataManager = mock(StaticMetadataManager.class);
		doReturn(metadataLocalProperties).when(applicationContext).getBean(MetadataLocalProperties.class);
		doReturn(staticMetadataManager).when(applicationContext).getBean(StaticMetadataManager.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext).thenReturn(applicationContext);
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
	public void testRun() throws URISyntaxException {
		APIConfig apiConfig = mock(APIConfig.class);
		doReturn("0.0.0.0").when(apiConfig).getBindIP();

		GlobalConfig globalConfig = mock(GlobalConfig.class);
		doReturn(apiConfig).when(globalConfig).getAPI();

		Configuration configuration = mock(Configuration.class);
		doReturn(globalConfig).when(configuration).getGlobal();

		doReturn(configuration).when(sdkContext).getConfig();

		doReturn(new URI("http://0.0.0.0/")).when(clientRequest).url();
		doReturn(new HttpHeaders()).when(clientRequest).headers();
		doReturn(HttpMethod.GET).when(clientRequest).method();
		ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
		doReturn(headers).when(clientResponse).headers();
		doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());

		EnhancedWebClientReporter reporter = new EnhancedWebClientReporter(reporterProperties, sdkContext, consumerAPI, circuitBreakAPI);
		ClientResponse clientResponse1 = reporter.filter(clientRequest, exchangeFunction).block();
		assertThat(clientResponse1).isEqualTo(clientResponse);

		doReturn(true).when(reporterProperties).isEnabled();
		ClientResponse clientResponse2 = reporter.filter(clientRequest, exchangeFunction).block();
		assertThat(clientResponse2).isEqualTo(clientResponse);

	}

}
