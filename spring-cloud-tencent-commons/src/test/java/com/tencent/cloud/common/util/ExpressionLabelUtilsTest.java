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

package com.tencent.cloud.common.util;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.ServletExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link ExpressionLabelUtils}.
 *
 * @author lepdou 2022-05-27
 */
@ExtendWith(MockitoExtension.class)
public class ExpressionLabelUtilsTest {

	@Test
	public void testExpressionLabel() {
		String validLabel1 = "${http.query.uid}";
		String validLabel2 = "${http.header.uid}";
		String validLabel3 = "${http.cookie.uid}";
		String validLabel4 = "${http.method}";
		String validLabel5 = "${http.uri}";
		String invalidLabel1 = "${http.queryuid}";
		String invalidLabel2 = "{http.query.uid}";
		String invalidLabel3 = "${http.query.uid";
		String invalidLabel4 = "$ {http.query.uid}";
		String invalidLabel5 = "${ http.query.uid}";
		String invalidLabel6 = "${query.uid}";
		String invalidLabel7 = "http.query.uid";
		String invalidLabel8 = "$${http.uri}";
		String invalidLabel9 = "#{http.uri}";

		assertThat(ExpressionLabelUtils.isExpressionLabel(validLabel1)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(validLabel2)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(validLabel3)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(validLabel4)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(validLabel5)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel1)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel2)).isFalse();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel3)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel4)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel5)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel6)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel7)).isFalse();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel8)).isTrue();
		assertThat(ExpressionLabelUtils.isExpressionLabel(invalidLabel9)).isFalse();
	}

	@Test
	public void testResolveHttpServletRequest() {
		String validLabel1 = "${http.query.uid}";
		String validLabel2 = "${http.header.uid}";
		String validLabel3 = "${http.cookie.uid}";
		String validLabel4 = "${http.method}";
		String validLabel5 = "${http.uri}";
		String invalidLabel1 = "${http.queryuid}";
		String invalidLabel2 = "{http.query.uid}";
		String invalidLabel3 = "${http.query.uid";
		String invalidLabel4 = "$ {http.query.uid}";
		String invalidLabel5 = "${ http.query.uid}";
		String invalidLabel6 = "${query.uid}";
		String invalidLabel7 = "http.query.uid";
		String invalidLabel8 = "$${http.uri}";
		String invalidLabel9 = "#{http.uri}";

		Set<String> labelKeys = Stream.of(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9).collect(toSet());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setQueryString("uid=zhangsan");
		request.addHeader("uid", "zhangsan");
		request.setCookies(new MockCookie("uid", "zhangsan"));
		request.setMethod(HttpMethod.GET.name());
		request.setRequestURI("/users");

		Map<String, String> result = ServletExpressionLabelUtils.resolve(request, labelKeys);

		assertThat(result.get(validLabel1)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel2)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel3)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel4)).isEqualTo("GET");
		assertThat(result.get(validLabel5)).isEqualTo("/users");
		assertThat(result.get(invalidLabel1)).isNull();
		assertThat(result.get(invalidLabel2)).isNull();
		assertThat(result.get(invalidLabel3)).isNull();
		assertThat(result.get(invalidLabel4)).isNull();
		assertThat(result.get(invalidLabel5)).isNull();
		assertThat(result.get(invalidLabel6)).isNull();
		assertThat(result.get(invalidLabel7)).isNull();
		assertThat(result.get(invalidLabel8)).isNull();
		assertThat(result.get(invalidLabel9)).isNull();
	}

	@Test
	public void testResolveServerWebExchange() {
		String validLabel1 = "${http.query.uid}";
		String validLabel2 = "${http.header.uid}";
		String validLabel3 = "${http.cookie.uid}";
		String validLabel4 = "${http.method}";
		String validLabel5 = "${http.uri}";
		String invalidLabel1 = "${http.queryuid}";
		String invalidLabel2 = "{http.query.uid}";
		String invalidLabel3 = "${http.query.uid";
		String invalidLabel4 = "$ {http.query.uid}";
		String invalidLabel5 = "${ http.query.uid}";
		String invalidLabel6 = "${query.uid}";
		String invalidLabel7 = "http.query.uid";
		String invalidLabel8 = "$${http.uri}";
		String invalidLabel9 = "#{http.uri}";

		Set<String> labelKeys = Stream.of(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9).collect(toSet());

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://calleeService/user/get?uid=zhangsan")
				.header("uid", "zhangsan")
				.cookie(new HttpCookie("uid", "zhangsan")).build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(httpRequest).build();

		Map<String, String> result = SpringWebExpressionLabelUtils.resolve(exchange, labelKeys);

		assertThat(result.get(validLabel1)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel2)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel3)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel4)).isEqualTo("GET");
		assertThat(result.get(validLabel5)).isEqualTo("/user/get");
		assertThat(result.get(invalidLabel1)).isNull();
		assertThat(result.get(invalidLabel2)).isNull();
		assertThat(result.get(invalidLabel3)).isNull();
		assertThat(result.get(invalidLabel4)).isNull();
		assertThat(result.get(invalidLabel5)).isNull();
		assertThat(result.get(invalidLabel6)).isNull();
		assertThat(result.get(invalidLabel7)).isNull();
		assertThat(result.get(invalidLabel8)).isNull();
		assertThat(result.get(invalidLabel9)).isNull();
	}

	@Test
	public void testResolveHttpRequest() {
		String validLabel1 = "${http.query.uid}";
		String validLabel2 = "${http.header.uid}";
		String validLabel3 = "${http.cookie.uid}";
		String validLabel4 = "${http.method}";
		String validLabel5 = "${http.uri}";
		String invalidLabel1 = "${http.queryuid}";
		String invalidLabel2 = "{http.query.uid}";
		String invalidLabel3 = "${http.query.uid";
		String invalidLabel4 = "$ {http.query.uid}";
		String invalidLabel5 = "${ http.query.uid}";
		String invalidLabel6 = "${query.uid}";
		String invalidLabel7 = "http.query.uid";
		String invalidLabel8 = "$${http.uri}";
		String invalidLabel9 = "#{http.uri}";

		Set<String> labelKeys = Stream.of(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9).collect(toSet());

		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setMethod(HttpMethod.GET);
		request.setURI(URI.create("http://calleeService/user/get?uid=zhangsan"));
		request.getHeaders().add("uid", "zhangsan");
		request.getHeaders().add("cookie", "uid=zhangsan; auth-token=hauigdfu8esgf8");

		Map<String, String> result = SpringWebExpressionLabelUtils.resolve(request, labelKeys);

		assertThat(result.get(validLabel1)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel2)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel3)).isEqualTo("zhangsan");
		assertThat(result.get(validLabel4)).isEqualTo("GET");
		assertThat(result.get(validLabel5)).isEqualTo("/user/get");
		assertThat(result.get(invalidLabel1)).isNull();
		assertThat(result.get(invalidLabel2)).isNull();
		assertThat(result.get(invalidLabel3)).isNull();
		assertThat(result.get(invalidLabel4)).isNull();
		assertThat(result.get(invalidLabel5)).isNull();
		assertThat(result.get(invalidLabel6)).isNull();
		assertThat(result.get(invalidLabel7)).isNull();
		assertThat(result.get(invalidLabel8)).isNull();
		assertThat(result.get(invalidLabel9)).isNull();
	}
}
