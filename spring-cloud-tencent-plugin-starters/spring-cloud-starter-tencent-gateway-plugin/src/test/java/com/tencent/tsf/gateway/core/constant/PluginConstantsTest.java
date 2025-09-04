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

import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PluginConstants}.
 *
 * @author Haotian Zhang
 */
public class PluginConstantsTest {

	@Test
	public void testTraceIdEnabledType() {
		assertThat(PluginConstants.TraceIdEnabledType.getTraceIdEnabledType("Y")).isEqualTo(PluginConstants.TraceIdEnabledType.Y);
		assertThat(PluginConstants.TraceIdEnabledType.getTraceIdEnabledType("N")).isEqualTo(PluginConstants.TraceIdEnabledType.N);
		assertThat(PluginConstants.TraceIdEnabledType.getTraceIdEnabledType("test")).isNull();

		assertThatNoException().isThrownBy(() -> {
			PluginConstants.TraceIdEnabledType.checkValidity("Y");
		});
		assertThatNoException().isThrownBy(() -> {
			PluginConstants.TraceIdEnabledType.checkValidity("N");
		});
		assertThatThrownBy(() -> {
			PluginConstants.TraceIdEnabledType.checkValidity("test");
		}).isExactlyInstanceOf(TsfGatewayException.class).hasMessageContaining("Tag插件TraceIdEnabled类型");

	}
}
