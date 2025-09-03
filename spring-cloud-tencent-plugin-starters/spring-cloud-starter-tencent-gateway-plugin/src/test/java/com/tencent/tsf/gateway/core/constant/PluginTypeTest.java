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

import java.util.Map;

import com.tencent.tsf.gateway.core.model.JwtPlugin;
import com.tencent.tsf.gateway.core.model.OAuthPlugin;
import com.tencent.tsf.gateway.core.model.RequestTransformerPlugin;
import com.tencent.tsf.gateway.core.model.TagPlugin;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PluginType}.
 *
 * @author Haotian Zhang
 */
public class PluginTypeTest {

	@Test
	public void testPluginType() {
		assertThat(PluginType.getPluginType("ReqTransformer")).isEqualTo(PluginType.REQ_TRANSFORMER);
		assertThat(PluginType.getPluginType("OAuth")).isEqualTo(PluginType.OAUTH);
		assertThat(PluginType.getPluginType("Jwt")).isEqualTo(PluginType.JWT);
		assertThat(PluginType.getPluginType("Tag")).isEqualTo(PluginType.TAG);
		assertThat(PluginType.getPluginType("Unknown")).isNull();
		assertThat(PluginType.getPluginType("")).isNull();

		Map<String, String> map = PluginType.toMap();
		assertThat(map.get("OAUTH")).isEqualTo("OAuth");
		assertThat(map.get("JWT")).isEqualTo("Jwt");
		assertThat(map.get("TAG")).isEqualTo("Tag");

		assertThat(PluginType.OAUTH.getPluginClazz()).isEqualTo(OAuthPlugin.class);
		assertThat(PluginType.JWT.getPluginClazz()).isEqualTo(JwtPlugin.class);
		assertThat(PluginType.TAG.getPluginClazz()).isEqualTo(TagPlugin.class);

		PluginType.JWT.setType("newType");
		assertThat(PluginType.JWT.getType()).isEqualTo("newType");
		PluginType.JWT.setPluginClazz(RequestTransformerPlugin.class);
		assertThat(PluginType.JWT.getPluginClazz()).isEqualTo(RequestTransformerPlugin.class);
	}
}
