/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.rpc.enhancement.plugin.assembly;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.net.HttpHeaders;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.RequestContext;

import org.springframework.http.HttpMethod;

/**
 * AssemblyRequestContext.
 *
 * @author sean yu
 */
public class AssemblyRequestContext implements RequestContext {

	private final EnhancedRequestContext requestContext;

	private final ServiceKey callerService;

	private final String callerIp;

	private final Map<String, String> cookies;

	public AssemblyRequestContext(EnhancedRequestContext requestContext, ServiceKey callerService, String callerIp) {
		this.requestContext = requestContext;
		this.callerService = callerService;
		this.callerIp = callerIp;
		this.cookies = new HashMap<>();
		List<String> allCookies =
				Optional.ofNullable(requestContext.getHttpHeaders().get(HttpHeaders.COOKIE))
						.orElse(new ArrayList<>())
						.stream()
						.flatMap(it -> Arrays.stream(it.split(";")))
						.toList();
		allCookies.forEach(cookie -> {
			String[] cookieKV = cookie.split("=");
			if (cookieKV.length == 2) {
				cookies.put(cookieKV[0], cookieKV[1]);
			}
		});
	}

	@Override
	public String getMethod() {
		return requestContext.getHttpMethod().name();
	}

	@Override
	public void setMethod(String method) {
		requestContext.setHttpMethod(HttpMethod.valueOf(method));
	}

	@Override
	public String getHeader(String key) {
		return requestContext.getHttpHeaders().getFirst(key);
	}

	@Override
	public void setHeader(String key, String value) {
		requestContext.getHttpHeaders().set(key, value);
	}

	@Override
	public Collection<String> listHeaderKeys() {
		return requestContext.getHttpHeaders().keySet();
	}

	@Override
	public String getCookie(String key) {
		return this.cookies.get(key);
	}

	@Override
	public void setCookie(String key, String value) {
		this.cookies.put(key, value);
	}

	@Override
	public Collection<String> listCookieKeys() {
		return this.cookies.keySet();
	}

	@Override
	public String getCallerIp() {
		return callerIp;
	}

	@Override
	public ServiceKey getCallerService() {
		return callerService;
	}

	@Override
	public URI getURI() {
		return requestContext.getUrl();
	}

}
