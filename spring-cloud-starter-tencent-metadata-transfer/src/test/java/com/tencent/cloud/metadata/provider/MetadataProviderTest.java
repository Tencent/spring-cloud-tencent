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

package com.tencent.cloud.metadata.provider;

import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import org.junit.Test;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ReactiveMetadataProvider} and {@link ServletMetadataProvider}.
 *
 * @author quan, Shedfree Wu
 */
public class MetadataProviderTest {

	private static final String notExistKey = "empty";

	@Test
	public void testReactiveMetadataProvider() {
		String header1 = "header1";
		String value1 = "value1";
		String queryKey = "qk1";
		String queryValue = "qv1";
		String cookieKey = "ck1";
		String cookieValue = "cv1";
		String path = "/echo/test";
		MockServerHttpRequest request = MockServerHttpRequest.get(path)
				.header(header1, value1)
				.queryParam(queryKey, queryValue)
				.cookie(new HttpCookie(cookieKey, cookieValue))
				.build();

		ReactiveMetadataProvider reactiveMetadataProvider = new ReactiveMetadataProvider(request);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, header1)).isEqualTo(value1);
		// com.tencent.polaris.metadata.core.manager.ComposeMetadataProvider.getRawMetadataMapValue need return null when key don't exist
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, notExistKey)).isNull();
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey)).isEqualTo(cookieValue);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, notExistKey)).isNull();
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey)).isEqualTo(queryValue);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, notExistKey)).isNull();
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(notExistKey, queryKey)).isNull();

		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo("GET");
		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo(path);
		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(notExistKey)).isNull();
	}

	@Test
	public void testServletMetadataProvider() {
		String notExistKey = "empty";

		String header1 = "header1";
		String value1 = "value1";
		String queryKey = "qk1";
		String queryValue = "qv1";
		String cookieKey = "ck1";
		String cookieValue = "cv1";
		String path = "/echo/test";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(header1, value1);
		request.setCookies(new MockCookie(cookieKey, cookieValue));
		request.setMethod(HttpMethod.GET.name());
		request.setRequestURI(path);
		request.setQueryString(queryKey + "=" + queryValue);

		ServletMetadataProvider servletMetadataProvider = new ServletMetadataProvider(request);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, header1)).isEqualTo(value1);
		// com.tencent.polaris.metadata.core.manager.ComposeMetadataProvider.getRawMetadataMapValue need return null when key don't exist
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, notExistKey)).isNull();
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey)).isEqualTo(cookieValue);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, notExistKey)).isNull();
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey)).isEqualTo(queryValue);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, notExistKey)).isNull();
		assertThat(servletMetadataProvider.getRawMetadataMapValue(notExistKey, queryKey)).isNull();

		assertThat(servletMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo("GET");
		assertThat(servletMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo(path);
		assertThat(servletMetadataProvider.getRawMetadataStringValue(notExistKey)).isNull();
	}
}
