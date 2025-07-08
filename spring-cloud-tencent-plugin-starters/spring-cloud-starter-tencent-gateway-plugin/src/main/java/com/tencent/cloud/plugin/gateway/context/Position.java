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

package com.tencent.cloud.plugin.gateway.context;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Position {
	/**
	 * Path position.
	 */
	PATH,
	/**
	 * Query position.
	 */
	QUERY,
	/**
	 * Header position.
	 */
	HEADER,

	/**
	 * HTTP COOKIE.
	 */
	COOKIE,

	/**
	 * TSF TAG，目前用于 Request Transformer Plugin 的改写流量.
	 */
	TSF_TAG;

	@JsonCreator
	public static Position fromString(String key) {
		for (Position position : Position.values()) {
			if (position.name().equalsIgnoreCase(key)) {
				return position;
			}
		}
		return null;
	}
}
