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

package com.tencent.tsf.gateway.core.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CommonStatus}.
 *
 * @author Haotian Zhang
 */
public class CommonStatusTest {

	@Test
	public void testGetStatus() {
		assertThat(CommonStatus.getStatus("enabled")).isEqualTo(CommonStatus.ENABLED);
		assertThat(CommonStatus.getStatus("disabled")).isEqualTo(CommonStatus.DISABLED);
		assertThat(CommonStatus.getStatus("test")).isNull();

		assertThat(CommonStatus.getStatus("enabled", "")).isEqualTo(CommonStatus.ENABLED);
		assertThat(CommonStatus.getStatus("disabled", "")).isEqualTo(CommonStatus.DISABLED);
		assertThat(CommonStatus.getStatus("test", "")).isNull();

		assertThat(CommonStatus.ENABLED.getStatus()).isEqualTo("enabled");
		assertThat(CommonStatus.DISABLED.getStatus()).isEqualTo("disabled");
	}
}
