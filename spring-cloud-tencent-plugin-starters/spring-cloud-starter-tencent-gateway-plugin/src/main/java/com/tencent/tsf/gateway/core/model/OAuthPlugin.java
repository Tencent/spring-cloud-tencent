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
 * @ClassName OAuthPlugin
 * @Description OAuth插件类，必须保证改类的成员属性都是String类型,不可以当vo类使用
 * @Author vmershen
 * @Date 2019/7/1 11:56
 * @Version 1.0
 */
public class OAuthPlugin extends PluginInfo {

	private static final long serialVersionUID = -8269345448912085323L;

	//验证token微服务名（包含命名空间），当选择微服务时才传入值，值的格式为：命名空间/服务名
	//例如：ns-xxxx/provider-demo
	private String tokenAuthServiceName;

	//验证token路径
	//1、当选择外部请求地址时，是完整url地址，例如：http://127.0.0.1:8080/oauth/token
	//2、当选择微服务时，是微服务的方法path，例如：/oauth/token
	private String tokenAuthUrl;

	//验证token请求方法
	private String tokenAuthMethod;

	//认证请求超时时间,单位:秒 范围:0~30
	private Integer expireTime;

	//重定向地址，非必填
	private String redirectUrl;

	//token携带位置，网关取token位置与发送认证请求时token位置一致
	private String tokenBaggagePosition;

	//token的key值
	private String tokenKeyName;

	//payload的映射参数名称
	private String payloadMappingName;

	//payload映射到后端服务的携带位置
	private String payloadMappingPosition;

	public String getTokenAuthServiceName() {
		return tokenAuthServiceName;
	}

	public void setTokenAuthServiceName(String tokenAuthServiceName) {
		this.tokenAuthServiceName = tokenAuthServiceName;
	}

	public String getTokenAuthUrl() {
		return tokenAuthUrl;
	}

	public void setTokenAuthUrl(String tokenAuthUrl) {
		this.tokenAuthUrl = tokenAuthUrl;
	}

	public String getTokenAuthMethod() {
		return tokenAuthMethod;
	}

	public void setTokenAuthMethod(String tokenAuthMethod) {
		this.tokenAuthMethod = tokenAuthMethod;
	}

	public Integer getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Integer expireTime) {
		this.expireTime = expireTime;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
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

	public String getPayloadMappingName() {
		return payloadMappingName;
	}

	public void setPayloadMappingName(String payloadMappingName) {
		this.payloadMappingName = payloadMappingName;
	}

	public String getPayloadMappingPosition() {
		return payloadMappingPosition;
	}

	public void setPayloadMappingPosition(String payloadMappingPosition) {
		this.payloadMappingPosition = payloadMappingPosition;
	}

	@Override
	@JsonIgnore
	public void check() {
		super.check();
		if (StringUtils.isEmpty(tokenAuthUrl)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenAuthUrl");
		}
		if (StringUtils.isEmpty(tokenAuthMethod) || !(tokenAuthMethod.equalsIgnoreCase("get") ||
				tokenAuthMethod.equalsIgnoreCase("post"))) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenAuthMethod");
		}
		if (StringUtils.isEmpty(tokenBaggagePosition) || !(tokenBaggagePosition.equalsIgnoreCase("query") ||
				tokenBaggagePosition.equalsIgnoreCase("header"))) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenBaggagePosition");
		}
		if (StringUtils.isEmpty(tokenKeyName)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "tokenKeyName");
		}

		if (!StringUtils.isEmpty(payloadMappingPosition)
				&& !payloadMappingPosition.equalsIgnoreCase("query")
				&& !payloadMappingPosition.equalsIgnoreCase("header")) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, "payloadMappingPosition");
		}
		if (expireTime == null || expireTime < 0 || expireTime > 30) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_INVALID, "expireTime");
		}
	}
}
