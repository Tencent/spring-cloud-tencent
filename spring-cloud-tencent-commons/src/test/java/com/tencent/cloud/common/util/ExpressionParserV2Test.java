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

import com.tencent.cloud.common.util.expresstion.ExpressionParserV2;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ExpressionParserV2}.
 * @author lepdou 2022-10-08
 */
public class ExpressionParserV2Test {

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

		ExpressionParserV2 parser = new ExpressionParserV2();

		assertThat(parser.isExpressionLabel(validLabel1)).isTrue();
		assertThat(parser.isExpressionLabel(validLabel2)).isTrue();
		assertThat(parser.isExpressionLabel(validLabel3)).isTrue();
		assertThat(parser.isExpressionLabel(validLabel4)).isTrue();
		assertThat(parser.isExpressionLabel(validLabel5)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel1)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel2)).isFalse();
		assertThat(parser.isExpressionLabel(invalidLabel3)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel4)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel5)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel6)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel7)).isFalse();
		assertThat(parser.isExpressionLabel(invalidLabel8)).isTrue();
		assertThat(parser.isExpressionLabel(invalidLabel9)).isFalse();

		assertThat(parser.isQueryLabel(validLabel1)).isFalse();
		assertThat(parser.isHeaderLabel(validLabel2)).isFalse();
		assertThat(parser.isCookieLabel(validLabel3)).isFalse();
		assertThat(parser.isMethodLabel(validLabel4)).isFalse();
		assertThat(parser.isUriLabel(validLabel5)).isFalse();

		assertThat(parser.isHeaderLabel("$header.userId")).isTrue();
		assertThat(parser.isMethodLabel("$method")).isTrue();
		assertThat(parser.isQueryLabel("$query.userId")).isTrue();
	}
}
