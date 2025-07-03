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

package com.tencent.tsf.gateway.core.util;

import java.util.Map;

import org.springframework.util.CollectionUtils;

/**
 * @author: vmershen
 * @description:
 * @create: 2020-05-23 17:41
 **/
public final class CookieUtil {

	private CookieUtil() {

	}

	public static void buildCookie(StringBuilder cookieStringBuilder, Map<String, String> requestCookieMap) {
		if (!CollectionUtils.isEmpty(requestCookieMap)) {
			for (Map.Entry<String, String> cookieEntry : requestCookieMap.entrySet()) {
				if (cookieStringBuilder.length() == 0) {
					cookieStringBuilder.append(cookieEntry.getKey() + "=" + cookieEntry.getValue());
					continue;
				}
				cookieStringBuilder.append("; ");
				cookieStringBuilder.append(cookieEntry.getKey() + "=" + cookieEntry.getValue());
			}
		}
	}
}
