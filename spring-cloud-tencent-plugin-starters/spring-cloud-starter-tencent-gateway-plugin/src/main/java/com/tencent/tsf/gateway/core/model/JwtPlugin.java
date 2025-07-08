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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;

/**
 * @ClassName JwtPlugin
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/3 17:15
 * @Version 1.0
 */
public class JwtPlugin extends PluginInfo {

	private static final long serialVersionUID = 2238976166882026848L;
	//公钥对kid
	private String kid;

	//公钥对
	private String publicKeyJson;

	//token携带位置
	private String tokenBaggagePosition;

	//token的key值,校验参数名称
	private String tokenKeyName;

	//重定向地址，非必填
	private String redirectUrl;

	//claim参数映射关系json
	private String claimMappingJson;

	public String getPublicKeyJson() {
		return publicKeyJson;
	}

	public void setPublicKeyJson(String publicKeyJson) {
		this.publicKeyJson = publicKeyJson;
	}

	public String getTokenBaggagePosition() {
		return tokenBaggagePosition;
	}

	public void setTokenBaggagePosition(String tokenBaggagePosition) {
		this.tokenBaggagePosition = tokenBaggagePosition;
	}

	public String getTokenKeyName() {
		return tokenKeyName;
	}

	public void setTokenKeyName(String tokenKeyName) {
		this.tokenKeyName = tokenKeyName;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public String getKid() {
		return kid;
	}

	public void setKid(String kid) {
		this.kid = kid;
	}

	public String getClaimMappingJson() {
		return claimMappingJson;
	}

	public void setClaimMappingJson(String claimMappingJson) {
		this.claimMappingJson = claimMappingJson;
	}

	@Override
	@JsonIgnore
	public void check() {
		super.check();
		if (StringUtils.isEmpty(publicKeyJson)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "publicKeyJson");
		}
		if (StringUtils.isEmpty(tokenBaggagePosition) || !(tokenBaggagePosition.equalsIgnoreCase("query") ||
				tokenBaggagePosition.equalsIgnoreCase("header"))) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenBaggagePosition");
		}
		if (StringUtils.isEmpty(tokenKeyName)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenKeyName");
		}
		if (StringUtils.isEmpty(kid)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "kid");
		}
	}
}
