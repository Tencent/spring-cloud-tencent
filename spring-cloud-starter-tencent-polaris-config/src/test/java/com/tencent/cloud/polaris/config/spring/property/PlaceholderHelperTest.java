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

package com.tencent.cloud.polaris.config.spring.property;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PlaceholderHelper}.
 *
 * @author lingxiao.wlx
 */
public class PlaceholderHelperTest {

	private static final PlaceholderHelper PLACEHOLDER_HELPER = new PlaceholderHelper();

	@Test
	public void extractNormalPlaceholderKeysTest() {
		final String placeholderCase = "${some.key}";
		final String placeholderCase1 = "${some.key:${some.other.key:100}}";
		final String placeholderCase2 = "${${some.key}}";
		final String placeholderCase3 = "${${some.key:other.key}}";
		final String placeholderCase4 = "${${some.key}:${another.key}}";
		final String placeholderCase5 = "#{new java.text.SimpleDateFormat('${some.key}').parse('${another.key}')}";

		Set<String> placeholderKeys = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase);
		assertThat(placeholderKeys.size()).isEqualTo(1);
		assertThat(placeholderKeys).contains("some.key");

		Set<String> placeholderKeys1 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase1);
		assertThat(placeholderKeys1.size()).isEqualTo(2);
		assertThat(placeholderKeys1).contains("some.key");
		assertThat(placeholderKeys1).contains("some.other.key");

		Set<String> placeholderKeys2 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase2);
		assertThat(placeholderKeys2.size()).isEqualTo(1);
		assertThat(placeholderKeys2).contains("some.key");

		Set<String> placeholderKeys3 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase3);
		assertThat(placeholderKeys3.size()).isEqualTo(1);
		assertThat(placeholderKeys3).contains("some.key");

		Set<String> placeholderKeys4 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase4);
		assertThat(placeholderKeys4.size()).isEqualTo(2);
		assertThat(placeholderKeys4).contains("some.key");
		assertThat(placeholderKeys4).contains("another.key");

		Set<String> placeholderKeys5 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase5);
		assertThat(placeholderKeys5.size()).isEqualTo(2);
		assertThat(placeholderKeys5).contains("some.key");
		assertThat(placeholderKeys5).contains("another.key");
	}

	@Test
	public void extractIllegalPlaceholderKeysTest() {
		final String placeholderCase = "${some.key";
		final String placeholderCase1 = "{some.key}";
		final String placeholderCase2 = "some.key";

		Set<String> placeholderKeys = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase);
		assertThat(placeholderKeys).isEmpty();

		Set<String> placeholderKeys1 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase1);
		assertThat(placeholderKeys1).isEmpty();

		Set<String> placeholderKeys2 = PLACEHOLDER_HELPER.extractPlaceholderKeys(placeholderCase2);
		assertThat(placeholderKeys2).isEmpty();
	}
}
