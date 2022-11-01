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

package com.tencent.cloud.common.util;

import com.tencent.cloud.common.util.expresstion.ExpressionParserV1;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link ExpressionParserV1}.
 * @author lepdou 2022-10-08
 */
public class ExpressionParserV1Test {

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

		ExpressionParserV1 parser = new ExpressionParserV1();

		Assert.assertTrue(parser.isExpressionLabel(validLabel1));
		Assert.assertTrue(parser.isExpressionLabel(validLabel2));
		Assert.assertTrue(parser.isExpressionLabel(validLabel3));
		Assert.assertTrue(parser.isExpressionLabel(validLabel4));
		Assert.assertTrue(parser.isExpressionLabel(validLabel5));
		Assert.assertTrue(parser.isExpressionLabel(invalidLabel1));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel2));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel3));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel4));
		Assert.assertTrue(parser.isExpressionLabel(invalidLabel5));
		Assert.assertTrue(parser.isExpressionLabel(invalidLabel6));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel7));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel8));
		Assert.assertFalse(parser.isExpressionLabel(invalidLabel9));

		Assert.assertTrue(parser.isQueryLabel(validLabel1));
		Assert.assertTrue(parser.isHeaderLabel(validLabel2));
		Assert.assertTrue(parser.isCookieLabel(validLabel3));
		Assert.assertTrue(parser.isMethodLabel(validLabel4));
		Assert.assertTrue(parser.isUriLabel(validLabel5));
	}
}
