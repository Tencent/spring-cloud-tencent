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
 *
 */

package com.tencent.cloud.metadata.core;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;

/**
 * According to request and trans-headers(key list in string type) in metadata, build
 * the complete headers(key-value list in map type) into metadata.
 *
 * @author lingxiao.wlx
 */
public final class TransHeadersTransfer {

	private TransHeadersTransfer() {
	}

	/**
	 * According to {@link HttpServletRequest} and trans-headers(key list in string type) in metadata, build
	 * the complete headers(key-value list in map type) into metadata.
	 *
	 * @param httpServletRequest httpServletRequest
	 */
	public static void transfer(HttpServletRequest httpServletRequest) {
		// transHeaderMetadata: for example, {"trans-headers" : {"header1,header2,header3":""}}
		Map<String, String> transHeaderMetadata = MetadataContextHolder.get().getTransHeaders();
		if (!CollectionUtils.isEmpty(transHeaderMetadata)) {
			String transHeaders = transHeaderMetadata.keySet().stream().findFirst().orElse("");
			String[] transHeaderArray = transHeaders.split(",");
			Enumeration<String> httpHeaders = httpServletRequest.getHeaderNames();
			while (httpHeaders.hasMoreElements()) {
				String httpHeader = httpHeaders.nextElement();
				Arrays.stream(transHeaderArray).forEach(transHeader -> {
					if (transHeader.equals(httpHeader)) {
						String httpHeaderValue = httpServletRequest.getHeader(httpHeader);
						// for example, {"trans-headers-kv" : {"header1":"v1","header2":"v2"...}}
						MetadataContextHolder.get().setTransHeadersKV(httpHeader, httpHeaderValue);
					}
				});
			}
		}
	}

	/**
	 * According to {@link ServerHttpRequest} and trans-headers(key list in string type) in metadata, build
	 * the complete headers(key-value list in map type) into metadata.
	 *
	 * @param serverHttpRequest serverHttpRequest
	 */
	public static void transfer(ServerHttpRequest serverHttpRequest) {
		// transHeaderMetadata: for example, {"trans-headers" : {"header1,header2,header3":""}}
		Map<String, String> transHeaderMetadata = MetadataContextHolder.get().getTransHeaders();
		if (!CollectionUtils.isEmpty(transHeaderMetadata)) {
			String transHeaders = transHeaderMetadata.keySet().stream().findFirst().orElse("");
			String[] transHeaderArray = transHeaders.split(",");
			HttpHeaders headers = serverHttpRequest.getHeaders();
			Set<String> headerKeys = headers.keySet();
			for (String httpHeader : headerKeys) {
				Arrays.stream(transHeaderArray).forEach(transHeader -> {
					if (transHeader.equals(httpHeader)) {
						List<String> list = headers.get(httpHeader);
						String httpHeaderValue = JacksonUtils.serialize2Json(list);
						// for example, {"trans-headers-kv" : {"header1":"v1","header2":"v2"...}}
						MetadataContextHolder.get().setTransHeadersKV(httpHeader, httpHeaderValue);
					}
				});
			}
		}
	}
}
