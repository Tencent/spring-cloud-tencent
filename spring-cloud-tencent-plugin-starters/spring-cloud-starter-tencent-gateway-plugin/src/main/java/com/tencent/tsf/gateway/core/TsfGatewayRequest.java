/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.tsf.gateway.core;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

import com.tencent.polaris.api.utils.StringUtils;

/**
 * @author kysonli
 * 2019/4/15 15:01
 */
public class TsfGatewayRequest {

	private URI uri;

	private String method;

	private Map<String, String> headers;

	/**
	 * 原始请求头，用于插件读取请求头信息.
	 */
	private Map<String, String> requestHeaders;

	private Map<String, String[]> parameterMap;

	private Map<String, String> cookieMap;

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}


	public String getHeader(String headerKey) {
		return headers.get(headerKey);
	}

	public Map<String, String[]> getParameterMap() {
		return parameterMap;
	}

	public void setParameterMap(Map<String, String[]> parameterMap) {
		this.parameterMap = parameterMap;
	}

	public Map<String, String> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(Map<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public String getRequestHeader(String headerKey) {
		return StringUtils.isNotEmpty(requestHeaders.get(headerKey)) ? requestHeaders.get(headerKey) :
				requestHeaders.get(headerKey.toLowerCase(Locale.ROOT));
	}

	public void putRequestHeader(String headerKey, String headerValue) {
		requestHeaders.put(headerKey.toLowerCase(Locale.ROOT), headerValue);
	}

	public Map<String, String> getCookieMap() {
		return cookieMap;
	}

	public void setCookieMap(Map<String, String> cookieMap) {
		this.cookieMap = cookieMap;
	}

	public String getCookie(String key) {
		return cookieMap.get(key);
	}

	public void putCookie(String key, String value) {
		cookieMap.put(key, value);
	}

	@Override
	public String toString() {
		return "TsfGatewayRequest{" +
				"uri=" + uri +
				", method='" + method + '\'' +
				", headers=" + headers +
				", requestHeaders=" + requestHeaders +
				", parameterMap=" + parameterMap +
				", cookieMap=" + cookieMap +
				'}';
	}
}
