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

package com.tencent.tsf.gateway.core.model;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PluginPayload}.
 *
 * @author Haotian Zhang
 */
public class PluginPayloadTest {

	@Test
	public void testPluginPayload() {
		PluginPayload pluginPayload = new PluginPayload();
		pluginPayload.setRequestHeaders(new HashMap<>());
		pluginPayload.getRequestHeaders().put("request", "test");
		pluginPayload.setResponseHeaders(new HashMap<>());
		pluginPayload.getResponseHeaders().put("response", "test");
		pluginPayload.setRequestCookies(new HashMap<>());
		pluginPayload.getRequestCookies().put("cookie", "test");
		pluginPayload.setParameterMap(new HashMap<>());
		pluginPayload.getParameterMap().put("param", new String[] {"test"});
		pluginPayload.setRedirectUrl("redirectUrl");

		assertThat(pluginPayload.getRedirectUrl()).isEqualTo("redirectUrl");
	}
}
