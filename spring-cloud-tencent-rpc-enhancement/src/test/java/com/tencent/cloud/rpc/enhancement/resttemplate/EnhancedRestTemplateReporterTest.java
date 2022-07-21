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


import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test For {@link EnhancedRestTemplateReporter}.
 *
 * @author wh 2022/6/22
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EnhancedRestTemplateReporterTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=Test", "spring.cloud.polaris.service=TestApp"})
public class EnhancedRestTemplateReporterTest {

	@Test
	public void handleError() throws Exception {
		ConsumerAPI consumerAPI = mock(ConsumerAPI.class);
		EnhancedRestTemplateReporter enhancedRestTemplateReporter =
				new EnhancedRestTemplateReporter(mock(RpcEnhancementProperties.class), consumerAPI);
		URI uri = mock(URI.class);
		when(uri.getPath()).thenReturn("/test");
		when(uri.getHost()).thenReturn("host");
		HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
		URL url = mock(URL.class);
		when(httpURLConnection.getURL()).thenReturn(url);
		when(url.getHost()).thenReturn("127.0.0.1");
		when(url.getPort()).thenReturn(8080);
		when(httpURLConnection.getResponseCode()).thenReturn(200);
		SimpleClientHttpResponseTest clientHttpResponse = new SimpleClientHttpResponseTest(httpURLConnection);
		enhancedRestTemplateReporter.handleError(uri, HttpMethod.GET, clientHttpResponse);
		when(consumerAPI.unWatchService(null)).thenReturn(true);
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
