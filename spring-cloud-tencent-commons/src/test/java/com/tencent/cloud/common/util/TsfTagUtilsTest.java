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

package com.tencent.cloud.common.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.tsf.core.entity.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link TsfTagUtils}.
 */
public class TsfTagUtilsTest {

	@Test
	public void deserializeTagList() {
		String data = "%5B%7B%22k%22%3A%22tsf-gateway-ratelimit-context%22%2C%22v%22%3A%22grp-vyiwvq5t%22%2C%22f%22%3A%5B%5D%7D%2C%7B%22k%22%3A%22feat%22%2C%22v%22%3A%22test%22%2C%22f%22%3A%5B%220%22%5D%7D%5D";
		List<Tag> tagList = TsfTagUtils.deserializeTagList(data);
		for (Tag tag : tagList) {
			assertThat(tag.getKey()).isNotNull();
			assertThat(tag.getValue()).isNotNull();
			assertThat(tag.getFlags()).isNotNull();
		}
	}
}
