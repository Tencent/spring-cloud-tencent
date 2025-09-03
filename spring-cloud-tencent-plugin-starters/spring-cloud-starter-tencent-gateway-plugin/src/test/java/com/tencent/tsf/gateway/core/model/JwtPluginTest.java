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

import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link JwtPlugin}.
 *
 * @author Haotian Zhang
 */
public class JwtPluginTest {

	@Test
	public void testJwtPlugin() {
		JwtPlugin jwtPlugin = new JwtPlugin();

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件名称参数错误");
		jwtPlugin.setName("jwt");

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件类型参数错误");
		jwtPlugin.setType("Jwt");

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("publicKeyJson");
		jwtPlugin.setPublicKeyJson("publicKeyJson");

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenBaggagePosition");
		jwtPlugin.setTokenBaggagePosition("tokenBaggagePosition");
		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenBaggagePosition");
		jwtPlugin.setTokenBaggagePosition("query");

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenKeyName");
		jwtPlugin.setTokenKeyName("tokenKeyName");

		assertThatThrownBy(jwtPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("kid");
		jwtPlugin.setKid("kid");

		assertThatNoException().isThrownBy(jwtPlugin::check);
		jwtPlugin.setRedirectUrl("redirectUrl");
		jwtPlugin.setClaimMappingJson("claimMappingJson");
		jwtPlugin.setTokenBaggagePosition("header");
		assertThatNoException().isThrownBy(jwtPlugin::check);

		assertThat(jwtPlugin.getKid()).isEqualTo("kid");
		assertThat(jwtPlugin.getPublicKeyJson()).isEqualTo("publicKeyJson");
		assertThat(jwtPlugin.getTokenBaggagePosition()).isEqualTo("header");
		assertThat(jwtPlugin.getTokenKeyName()).isEqualTo("tokenKeyName");
		assertThat(jwtPlugin.getRedirectUrl()).isEqualTo("redirectUrl");
		assertThat(jwtPlugin.getClaimMappingJson()).isEqualTo("claimMappingJson");
	}
}
