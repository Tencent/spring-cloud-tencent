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

package com.tencent.cloud.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Utils for URLDecoder/URLEncoder.
 *
 * @author Shedfree Wu
 */
public final class UrlUtils {

	private static final Logger LOG = LoggerFactory.getLogger(UrlUtils.class);

	private UrlUtils() {
	}

	public static String decode(String s) {
		return decode(s, UTF_8);
	}

	public static String decode(String s, String enc) {
		if (!StringUtils.hasText(s)) {
			return s;
		}
		try {
			return URLDecoder.decode(s, enc);
		}
		catch (UnsupportedEncodingException e) {
			LOG.warn("Runtime system does not support {} coding. s:{}, msg:{}", enc, s, e.getMessage());
			// return original string
			return s;
		}
	}

	public static String encode(String s) {
		return encode(s, UTF_8);
	}

	public static String encode(String s, String enc) {
		if (!StringUtils.hasText(s)) {
			return s;
		}
		try {
			return URLEncoder.encode(s, enc);
		}
		catch (UnsupportedEncodingException e) {
			LOG.warn("Runtime system does not support {} coding. s:{}, msg:{}", enc, s, e.getMessage());
			// return original string
			return s;
		}
	}
}
