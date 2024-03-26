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

import com.tencent.cloud.common.util.UrlUtils;
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
		String headerKey1 = "header1";
		String headerKey2 = "header2";
		String headerValue1 = "value1";
		String headerValue2 = "value2/test";
		String queryKey1 = "qk1";
		String queryKey2 = "qk2";
		String queryValue1 = "qv1";
		String queryValue2 = "qv2/test";
		String cookieKey1 = "ck1";
		String cookieKey2 = "ck2";
		String cookieValue1 = "cv1";
		String cookieValue2 = "cv2/test";
		String path = "/echo/test";
		MockServerHttpRequest request = MockServerHttpRequest.get(path)
				.header(headerKey1, headerValue1)
				.header(headerKey2, UrlUtils.encode(headerValue2))
				.queryParam(queryKey1, queryValue1)
				.queryParam(queryKey2, UrlUtils.encode(queryValue2))
				.cookie(new HttpCookie(cookieKey1, cookieValue1))
				.cookie(new HttpCookie(cookieKey2, UrlUtils.encode(cookieValue2)))
				.build();

		ReactiveMetadataProvider reactiveMetadataProvider = new ReactiveMetadataProvider(request);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, headerKey1)).isEqualTo(headerValue1);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, headerKey2)).isEqualTo(headerValue2);
		// com.tencent.polaris.metadata.core.manager.ComposeMetadataProvider.getRawMetadataMapValue need return null when key don't exist
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, notExistKey)).isNull();

		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey1)).isEqualTo(cookieValue1);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey2)).isEqualTo(cookieValue2);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, notExistKey)).isNull();

		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey1)).isEqualTo(queryValue1);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey2)).isEqualTo(queryValue2);
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, notExistKey)).isNull();
		assertThat(reactiveMetadataProvider.getRawMetadataMapValue(notExistKey, queryKey1)).isNull();

		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo("GET");
		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo(path);
		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(notExistKey)).isNull();

		request = MockServerHttpRequest.get("/echo/" + UrlUtils.decode("a@b")).build();
		reactiveMetadataProvider = new ReactiveMetadataProvider(request);
		assertThat(reactiveMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/echo/a@b");
	}

	@Test
	public void testServletMetadataProvider() {
		String headerKey1 = "header1";
		String headerKey2 = "header2";
		String headerValue1 = "value1";
		String headerValue2 = "value2/test";
		String queryKey1 = "qk1";
		String queryKey2 = "qk2";
		String queryValue1 = "qv1";
		String queryValue2 = "qv2/test";
		String cookieKey1 = "ck1";
		String cookieKey2 = "ck2";
		String cookieValue1 = "cv1";
		String cookieValue2 = "cv2/test";
		String path = "/echo/test";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerKey1, headerValue1);
		request.addHeader(headerKey2, UrlUtils.encode(headerValue2));
		request.setCookies(new MockCookie(cookieKey1, cookieValue1), new MockCookie(cookieKey2, UrlUtils.encode(cookieValue2)));
		request.setMethod(HttpMethod.GET.name());
		request.setRequestURI(path);
		request.setQueryString(queryKey1 + "=" + queryValue1 + "&" + queryKey2 + "=" + UrlUtils.encode(queryValue2));

		ServletMetadataProvider servletMetadataProvider = new ServletMetadataProvider(request);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, headerKey1)).isEqualTo(headerValue1);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, headerKey2)).isEqualTo(headerValue2);
		// com.tencent.polaris.metadata.core.manager.ComposeMetadataProvider.getRawMetadataMapValue need return null when key don't exist
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, notExistKey)).isNull();

		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey1)).isEqualTo(cookieValue1);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, cookieKey2)).isEqualTo(cookieValue2);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, notExistKey)).isNull();

		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey1)).isEqualTo(queryValue1);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, queryKey2)).isEqualTo(queryValue2);
		assertThat(servletMetadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, notExistKey)).isNull();
		assertThat(servletMetadataProvider.getRawMetadataMapValue(notExistKey, queryKey1)).isNull();

		assertThat(servletMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo("GET");
		assertThat(servletMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo(path);
		assertThat(servletMetadataProvider.getRawMetadataStringValue(notExistKey)).isNull();

		request.setRequestURI("/echo/" + UrlUtils.decode("a@b"));
		assertThat(servletMetadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/echo/a@b");
	}
}
