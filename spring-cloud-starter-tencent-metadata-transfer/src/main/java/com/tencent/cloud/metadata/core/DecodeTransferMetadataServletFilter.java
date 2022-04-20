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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter used for storing the metadata from upstream temporarily when web application is
 * SERVLET.
 *
 * @author Haotian Zhang
 */
@Order(MetadataConstant.OrderConstant.WEB_FILTER_ORDER)
public class DecodeTransferMetadataServletFilter extends OncePerRequestFilter {

	private static final Logger LOG = LoggerFactory.getLogger(DecodeTransferMetadataServletFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
			FilterChain filterChain) throws ServletException, IOException {
		// Get custom metadata string from http header.
		String customMetadataStr = httpServletRequest.getHeader(MetadataConstant.HeaderName.CUSTOM_METADATA);
		try {
			if (StringUtils.hasText(customMetadataStr)) {
				customMetadataStr = URLDecoder.decode(customMetadataStr, "UTF-8");
			}
		}
		catch (UnsupportedEncodingException e) {
			LOG.error("Runtime system does not support utf-8 coding.", e);
		}
		LOG.debug("Get upstream metadata string: {}", customMetadataStr);

		// create custom metadata.
		Map<String, String> upstreamCustomMetadataMap = JacksonUtils.deserialize2Map(customMetadataStr);

		try {
			MetadataContextHolder.init(upstreamCustomMetadataMap, null);

			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}
		catch (IOException | ServletException | RuntimeException e) {
			throw e;
		}
		finally {
			MetadataContextHolder.remove();
		}
	}

}
