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

package com.tencent.tsf.gateway.core.constant;

/**
 * @author kysonli
 * 2019/4/11 11:54
 */
public enum HttpMethod {
	/**
	 * POST.
	 */
	POST("POST"),
	/**
	 * GET.
	 */
	GET("GET"),
	/**
	 * PUT.
	 */
	PUT("PUT"),
	/**
	 * DELETE.
	 */
	DELETE("DELETE");

	private final String httpMethod;

	HttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public static HttpMethod getHttpMethod(String method) {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			if (httpMethod.httpMethod.equalsIgnoreCase(method)) {
				return httpMethod;
			}
		}
		return null;
	}
}
