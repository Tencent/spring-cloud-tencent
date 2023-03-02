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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link AddressUtils}.
 *
 * @author lepdou 2022-05-27
 */
@ExtendWith(MockitoExtension.class)
public class AddressUtilsTest {

	@Test
	public void testEmptyStr() {
		List<String> result = AddressUtils.parseAddressList("");
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	public void testNullStr() {
		List<String> result = AddressUtils.parseAddressList(null);
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	@Test
	public void testOneStr() {
		String host1 = "http://localhost";
		List<String> result = AddressUtils.parseAddressList(host1);
		assertThat(result.size()).isEqualTo(1);
		assertThat(result).contains("localhost");
	}

	@Test
	public void testMultiStr() {
		String host1 = "http://localhost";
		String host2 = "http://localhost2";
		String host3 = "http://localhost3";
		List<String> result = AddressUtils.parseAddressList(host1 + "," + host2 + "," + host3);
		assertThat(result.size()).isEqualTo(3);
		assertThat(result).contains("localhost");
		assertThat(result).contains("localhost2");
		assertThat(result).contains("localhost3");
	}
}
