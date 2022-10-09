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

import com.google.common.collect.Sets;
import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.ServletExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * test for {@link ExpressionLabelUtils}.
 *
 * @author lepdou 2022-05-27, cheese8
 */
@RunWith(MockitoJUnitRunner.class)
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

		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(validLabel1));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(validLabel2));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(validLabel3));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(validLabel4));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(validLabel5));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(invalidLabel1));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel2));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel3));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel4));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(invalidLabel5));
		Assert.assertTrue(ExpressionLabelUtils.isExpressionLabel(invalidLabel6));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel7));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel8));
		Assert.assertFalse(ExpressionLabelUtils.isExpressionLabel(invalidLabel9));
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

		Set<String> labelKeys = Sets.newHashSet(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setQueryString("uid=zhangsan");
		request.addHeader("uid", "zhangsan");
		request.setCookies(new MockCookie("uid", "zhangsan"));
		request.setMethod(HttpMethod.GET.name());
		request.setRequestURI("/users");

		Map<String, String> result = ServletExpressionLabelUtils.resolve(request, labelKeys);

		Assert.assertEquals("zhangsan", result.get(validLabel1));
		Assert.assertEquals("zhangsan", result.get(validLabel2));
		Assert.assertEquals("zhangsan", result.get(validLabel3));
		Assert.assertEquals("GET", result.get(validLabel4));
		Assert.assertEquals("/users", result.get(validLabel5));
		Assert.assertNull(result.get(invalidLabel1));
		Assert.assertNull(result.get(invalidLabel2));
		Assert.assertNull(result.get(invalidLabel3));
		Assert.assertNull(result.get(invalidLabel4));
		Assert.assertNull(result.get(invalidLabel5));
		Assert.assertNull(result.get(invalidLabel6));
		Assert.assertNull(result.get(invalidLabel7));
		Assert.assertNull(result.get(invalidLabel8));
		Assert.assertNull(result.get(invalidLabel9));
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

		Set<String> labelKeys = Sets.newHashSet(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://calleeService/user/get?uid=zhangsan")
				.header("uid", "zhangsan")
				.cookie(new HttpCookie("uid", "zhangsan")).build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(httpRequest).build();

		Map<String, String> result = SpringWebExpressionLabelUtils.resolve(exchange, labelKeys);

		Assert.assertEquals("zhangsan", result.get(validLabel1));
		Assert.assertEquals("zhangsan", result.get(validLabel2));
		Assert.assertEquals("zhangsan", result.get(validLabel3));
		Assert.assertEquals("GET", result.get(validLabel4));
		Assert.assertEquals("/user/get", result.get(validLabel5));
		Assert.assertNull(result.get(invalidLabel1));
		Assert.assertNull(result.get(invalidLabel2));
		Assert.assertNull(result.get(invalidLabel3));
		Assert.assertNull(result.get(invalidLabel4));
		Assert.assertNull(result.get(invalidLabel5));
		Assert.assertNull(result.get(invalidLabel6));
		Assert.assertNull(result.get(invalidLabel7));
		Assert.assertNull(result.get(invalidLabel8));
		Assert.assertNull(result.get(invalidLabel9));
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

		Set<String> labelKeys = Sets.newHashSet(validLabel1, validLabel2, validLabel3, validLabel4, validLabel5,
				invalidLabel1, invalidLabel2, invalidLabel3, invalidLabel4, invalidLabel5, invalidLabel6, invalidLabel7,
				invalidLabel8, invalidLabel9);

		MockClientHttpRequest request = new MockClientHttpRequest();
		request.setMethod(HttpMethod.GET);
		request.setURI(URI.create("http://calleeService/user/get?uid=zhangsan"));
		request.getHeaders().add("uid", "zhangsan");

		Map<String, String> result = SpringWebExpressionLabelUtils.resolve(request, labelKeys);

		Assert.assertEquals("zhangsan", result.get(validLabel1));
		Assert.assertEquals("zhangsan", result.get(validLabel2));
		Assert.assertNull(result.get(validLabel3));
		Assert.assertEquals("GET", result.get(validLabel4));
		Assert.assertEquals("/user/get", result.get(validLabel5));
		Assert.assertNull(result.get(invalidLabel1));
		Assert.assertNull(result.get(invalidLabel2));
		Assert.assertNull(result.get(invalidLabel3));
		Assert.assertNull(result.get(invalidLabel4));
		Assert.assertNull(result.get(invalidLabel5));
		Assert.assertNull(result.get(invalidLabel6));
		Assert.assertNull(result.get(invalidLabel7));
		Assert.assertNull(result.get(invalidLabel8));
		Assert.assertNull(result.get(invalidLabel9));
	}
}
