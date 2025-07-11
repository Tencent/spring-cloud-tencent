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

package com.tencent.cloud.rpc.enhancement.utils;

import java.net.URI;

import com.tencent.polaris.api.utils.ClassUtils;
import feign.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpRequest;

/**
 * Utils for enhanced request.
 *
 * @author Haotian Zhang
 */
public final class EnhancedRequestUtils {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedRequestUtils.class);

	private EnhancedRequestUtils() {

	}

	/**
	 * Get uri from original request.
	 *
	 * @param originalRequest original request
	 * @return URI
	 */
	public static URI getUri(Object originalRequest) {
		URI uri = null;
		if (ClassUtils.isClassPresent("org.springframework.http.HttpRequest")
				&& originalRequest instanceof HttpRequest) {
			HttpRequest httpRequest = (HttpRequest) originalRequest;
			uri = httpRequest.getURI();
			LOG.debug("Get uri: {} from org.springframework.http.HttpRequest", uri);
		}
		else if (ClassUtils.isClassPresent("feign.Request")
				&& originalRequest instanceof Request) {
			Request request = (Request) originalRequest;
			uri = URI.create(request.url());
			LOG.debug("Get uri: {} from feign.Request", uri);
		}
		return uri;
	}

	/**
	 * Get method from original request.
	 *
	 * @param originalRequest original request
	 * @return method
	 */
	public static String getMethod(Object originalRequest) {
		String method = "GET";
		if (ClassUtils.isClassPresent("org.springframework.http.HttpRequest")
				&& originalRequest instanceof HttpRequest) {
			method = ((HttpRequest) originalRequest).getMethod().name();
			LOG.debug("Get method: {} from org.springframework.http.HttpRequest", method);
		}
		else if (ClassUtils.isClassPresent("feign.Request")
				&& originalRequest instanceof Request) {
			method = ((Request) originalRequest).httpMethod().name();
			LOG.debug("Get method: {} from feign.Request", method);
		}
		return method;
	}
}
