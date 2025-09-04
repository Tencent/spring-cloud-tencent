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
 * Test for {@link OAuthPlugin}.
 *
 * @author Haotian Zhang
 */
public class OAuthPluginTest {

	@Test
	public void testOAuthPlugin() {
		OAuthPlugin oAuthPlugin = new OAuthPlugin();

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件名称参数错误");
		oAuthPlugin.setName("oauth");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("插件类型参数错误");
		oAuthPlugin.setType("OAuth");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenAuthUrl");
		oAuthPlugin.setTokenAuthUrl("tokenAuthUrl");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenAuthMethod");
		oAuthPlugin.setTokenAuthMethod("tokenAuthMethod");
		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenAuthMethod");
		oAuthPlugin.setTokenAuthMethod("get");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenBaggagePosition");
		oAuthPlugin.setTokenBaggagePosition("tokenBaggagePosition");
		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenBaggagePosition");
		oAuthPlugin.setTokenBaggagePosition("query");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("tokenKeyName");
		oAuthPlugin.setTokenKeyName("tokenKeyName");

		oAuthPlugin.setPayloadMappingPosition("payloadMappingPosition");
		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("payloadMappingPosition");
		oAuthPlugin.setPayloadMappingPosition("query");

		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("expireTime");
		oAuthPlugin.setExpireTime(100);
		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("expireTime");
		oAuthPlugin.setExpireTime(-100);
		assertThatThrownBy(oAuthPlugin::check)
				.isExactlyInstanceOf(TsfGatewayException.class)
				.hasMessageContaining("expireTime");
		oAuthPlugin.setExpireTime(15);


		oAuthPlugin.setTokenAuthServiceName("tokenAuthServiceName");
		oAuthPlugin.setRedirectUrl("redirectUrl");
		oAuthPlugin.setTokenKeyName("tokenKeyName");
		oAuthPlugin.setPayloadMappingName("payloadMappingName");

		assertThatNoException().isThrownBy(oAuthPlugin::check);
		oAuthPlugin.setTokenAuthMethod("post");
		oAuthPlugin.setPayloadMappingPosition("header");
		assertThatNoException().isThrownBy(oAuthPlugin::check);

		assertThat(oAuthPlugin.getTokenAuthServiceName()).isEqualTo("tokenAuthServiceName");
		assertThat(oAuthPlugin.getTokenAuthUrl()).isEqualTo("tokenAuthUrl");
		assertThat(oAuthPlugin.getTokenAuthMethod()).isEqualTo("post");
		assertThat(oAuthPlugin.getExpireTime()).isEqualTo(15);
		assertThat(oAuthPlugin.getRedirectUrl()).isEqualTo("redirectUrl");
		assertThat(oAuthPlugin.getTokenBaggagePosition()).isEqualTo("query");
		assertThat(oAuthPlugin.getTokenKeyName()).isEqualTo("tokenKeyName");
		assertThat(oAuthPlugin.getPayloadMappingName()).isEqualTo("payloadMappingName");
		assertThat(oAuthPlugin.getPayloadMappingPosition()).isEqualTo("header");
	}
}
