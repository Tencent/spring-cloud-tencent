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

import java.util.Map;

/**
 * @ClassName PluginPayload
 * @Description 插件认证完成后，附带到请求服务链路的值
 * @Author vmershen
 * @Date 2019/7/8 16:21
 * @Version 1.0
 */
public class PluginPayload {

	/**
	 * 附带到请求头的值.
	 */
	private Map<String, String> requestHeaders;

	/**
	 * 附带至响应头的值.
	 */
	private Map<String, String> responseHeaders;

	/**
	 * 附带至请求cookie的值.
	 */
	private Map<String, String> requestCookies;

	/**
	 * 附带至请求参数的值.
	 */
	private Map<String, String[]> parameterMap;

	private String redirectUrl;

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public Map<String, String[]> getParameterMap() {
		return parameterMap;
	}

	public void setParameterMap(Map<String, String[]> parameterMap) {
		this.parameterMap = parameterMap;
	}

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public Map<String, String> getRequestCookies() {
		return requestCookies;
	}

	public void setRequestCookies(Map<String, String> requestCookies) {
		this.requestCookies = requestCookies;
	}

	@Override
	public String toString() {
		return "PluginPayload{" +
				"requestHeaders=" + requestHeaders +
				", responseHeaders=" + responseHeaders +
				", requestCookies=" + requestCookies +
				", parameterMap=" + parameterMap +
				", redirectUrl='" + redirectUrl + '\'' +
				'}';
	}
}
