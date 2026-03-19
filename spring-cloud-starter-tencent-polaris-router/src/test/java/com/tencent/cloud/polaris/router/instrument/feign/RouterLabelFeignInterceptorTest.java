/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.polaris.router.instrument.feign;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.Request;
import feign.RequestTemplate;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * test for {@link RouterLabelFeignInterceptor}.
 *
 * @author lepdou 2022-05-26
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RouterLabelFeignInterceptorTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=test", "spring.application.name=test", "spring.cloud.gateway.enabled=false",
				"spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration,org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration,org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration"})
public class RouterLabelFeignInterceptorTest {

	@Test
	public void testRouterLabel() {
		RouterLabelFeignInterceptor routerLabelFeignInterceptor = new RouterLabelFeignInterceptor();

		assertThat(routerLabelFeignInterceptor.getOrder()).isEqualTo(OrderConstant.Client.Feign.ROUTER_LABEL_INTERCEPTOR_ORDER);

		// mock request template
		RequestTemplate requestTemplate = mock(RequestTemplate.class);
		String headerUidKey = "uid";
		String headerUidValue = "1000";
		Map<String, List<String>> headerMap = new HashMap<>();
		headerMap.put(headerUidKey, Collections.singletonList(headerUidValue));
		headerMap.put(HttpHeaderNames.COOKIE.toString(), Collections.singletonList("k1=v1"));
		doReturn(headerMap).when(requestTemplate).headers();
		doReturn(Request.HttpMethod.POST.toString()).when(requestTemplate).method();
		Request request = mock(Request.class);
		doReturn(request).when(requestTemplate).request();
		doReturn("http://callee/test/path").when(request).url();
		Map<String, List<String>> queryMap = new HashMap<>();
		queryMap.put("q1", Collections.singletonList("a1"));
		doReturn(queryMap).when(requestTemplate).queries();

		routerLabelFeignInterceptor.apply(requestTemplate);

		// get message metadata container
		MetadataContainer metadataContainer = MetadataContextHolder.get()
				.getMetadataContainer(MetadataType.MESSAGE, false);
		// method
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo(Request.HttpMethod.POST.toString());
		// path
		assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/test/path");
		// header
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, headerUidKey)).isEqualTo(headerUidValue);
		// cookie
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, "k1")).isEqualTo("v1");
		// query
		assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, "q1")).isEqualTo("a1");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
