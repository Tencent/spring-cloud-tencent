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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

	private OkHttpUtil() {

	}

	/**
	 * get request.
	 * @param path path
	 * @param headers headers
	 * @return response
	 */
	public static boolean get(String path, Map<String, String> headers) {
		HttpURLConnection conn = null;
		try {
			URL url = new java.net.URL(path);
			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(2));
			conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(2));
			if (!CollectionUtils.isEmpty(headers)) {
				headers.forEach(conn::setRequestProperty);
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			String str;
			while ((str = reader.readLine()) != null) {
				buffer.append(str);
			}
			String responseBody = buffer.toString();
			if (conn.getResponseCode() == 200 && StringUtils.hasText(responseBody)) {
				LOGGER.debug("exec get request, url: {} success, response data: {}", url, responseBody);
				return true;
			}
		}
		catch (Exception e) {
			LOGGER.error("exec get request, url: {} failed!", path, e);
			return false;
		}
		finally {
			if (null != conn) {
				conn.disconnect();
			}
		}
		return false;
	}
}
