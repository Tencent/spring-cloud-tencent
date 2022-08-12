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

package com.tencent.cloud.polaris.util;

import java.util.Map;
import java.util.Objects;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * okhttp util.
 *
 * @author kan peng
 */
public final class OkHttpUtil {

	/**
	 * Logger.
	 */
	public final static Logger LOGGER = LoggerFactory.getLogger(OkHttpUtil.class);

	/**
	 * client.
	 */
	private final static OkHttpClient HTTP_CLIENT = new OkHttpClient();

	private OkHttpUtil() {

	}

	/**
	 * get request.
	 * @param url url
	 * @param headers headers
	 * @return response
	 */
	public static boolean get(String url, Map<String, String> headers) {
		try {
			Request.Builder builder = new Request.Builder();
			buildHeader(builder, headers);
			Request request = builder.url(url).build();
			Response response = HTTP_CLIENT.newCall(request).execute();

			if (response.isSuccessful() && Objects.nonNull(response.body())) {
				String result = response.body().string();
				LOGGER.debug("exec get request, url: {} success，response data: {}", url, result);
				return true;
			}
		}
		catch (Exception e) {
			LOGGER.error("exec get request，url: {} failed!", url, e);
		}
		return false;
	}

	/**
	 * build header.
	 * @param builder builder
	 * @param headers headers
	 */
	private static void buildHeader(Request.Builder builder,
			Map<String, String> headers) {
		if (Objects.nonNull(headers) && headers.size() > 0) {
			headers.forEach((k, v) -> {
				if (Objects.nonNull(k) && Objects.nonNull(v)) {
					builder.addHeader(k, v);
				}
			});
		}
	}
}
