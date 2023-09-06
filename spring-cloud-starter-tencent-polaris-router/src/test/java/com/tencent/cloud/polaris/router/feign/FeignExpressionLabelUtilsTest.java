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

package com.tencent.cloud.polaris.router.feign;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FeignExpressionLabelUtils}.
 *@author lepdou 2022-05-26
 */
public class FeignExpressionLabelUtilsTest {

	@Test
	public void testGetHeaderLabel() {
		String headerKey = "uid";
		String headerValue = "1000";
		String headerKey2 = "teacher.age";
		String headerValue2 = "1000";

		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.header(headerKey, headerValue);
		requestTemplate.header(headerKey2, headerValue2);

		String labelKey1 = "${http.header.uid}";
		String labelKey2 = "${http.header.name}";
		String labelKey3 = "${http.headername}";
		String labelKey4 = "${http.header.}";
		String labelKey5 = "${http.header.teacher.age}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate,
				Stream.of(labelKey1, labelKey2, labelKey3, labelKey4, labelKey5).collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo(headerValue);
		assertThat(result.get(labelKey5)).isEqualTo(headerValue2);
		assertThat(result.get(labelKey2)).isBlank();
		assertThat(result.get(labelKey3)).isBlank();
		assertThat(result.get(labelKey4)).isBlank();
	}

	@Test
	public void testGetQueryLabel() {
		String headerKey = "uid";
		String headerValue = "1000";
		String headerKey2 = "teacher.age";
		String headerValue2 = "1000";

		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.query(headerKey, headerValue);
		requestTemplate.query(headerKey2, headerValue2);

		String labelKey1 = "${http.query.uid}";
		String labelKey2 = "${http.query.name}";
		String labelKey3 = "${http.queryname}";
		String labelKey4 = "${http.query.}";
		String labelKey5 = "${http.query.teacher.age}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate,
				Stream.of(labelKey1, labelKey2, labelKey3, labelKey4, labelKey5).collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo(headerValue);
		assertThat(result.get(labelKey5)).isEqualTo(headerValue2);
		assertThat(result.get(labelKey2)).isBlank();
		assertThat(result.get(labelKey3)).isBlank();
		assertThat(result.get(labelKey4)).isBlank();
	}

	@Test
	public void testGetMethod() {
		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.method(Request.HttpMethod.GET);

		String labelKey1 = "${http.method}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate, Stream.of(labelKey1)
				.collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo("GET");
	}

	@Test
	public void testGetUri() {
		String uri = "/user/get";

		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.uri(uri);
		requestTemplate.method(Request.HttpMethod.GET);
		requestTemplate.target("http://localhost");
		requestTemplate = requestTemplate.resolve(new HashMap<>());

		String labelKey1 = "${http.uri}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate, Stream.of(labelKey1)
				.collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo(uri);
	}

	@Test
	public void testGetUri2() {
		String uri = "/";

		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.uri(uri);
		requestTemplate.method(Request.HttpMethod.GET);
		requestTemplate.target("http://localhost");
		requestTemplate = requestTemplate.resolve(new HashMap<>());

		String labelKey1 = "${http.uri}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate, Stream.of(labelKey1)
				.collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo(uri);
	}

	@Test
	public void testGetCookie() {
		String uri = "/";
		String cookieValue = "zhangsan";

		RequestTemplate requestTemplate = new RequestTemplate();
		requestTemplate.uri(uri);
		requestTemplate.method(Request.HttpMethod.GET);
		requestTemplate.target("http://localhost");
		requestTemplate = requestTemplate.resolve(new HashMap<>());
		requestTemplate.header("cookie", Collections.singleton("uid=zhangsan; auth-token=dfhuwshfy77"));

		String labelKey1 = "${http.cookie.uid}";
		Map<String, String> result = FeignExpressionLabelUtils.resolve(requestTemplate, Stream.of(labelKey1)
				.collect(toSet()));

		assertThat(result).isNotEmpty();
		assertThat(result.get(labelKey1)).isEqualTo(cookieValue);
	}
}
