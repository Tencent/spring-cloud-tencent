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

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Utils for Zuul filter.
 *
 * @author Haotian Zhang
 */
public final class ZuulFilterUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZuulFilterUtils.class);

	private ZuulFilterUtils() {
	}

	public static String getServiceId(RequestContext context) {
		String serviceId = (String) context.get(SERVICE_ID_KEY);
		if (StringUtils.isBlank(serviceId)) {
			URL url = context.getRouteHost();
			if (url != null) {
				serviceId = url.getAuthority();
				context.set(SERVICE_ID_KEY, serviceId);
			}
		}
		return serviceId;
	}

	public static String getPath(RequestContext context) {
		HttpServletRequest request = context.getRequest();
		String uri = request.getRequestURI();
		String contextURI = (String) context.get(REQUEST_URI_KEY);
		if (contextURI != null) {
			try {
				uri = UriUtils.encodePath(contextURI, characterEncoding(request));
			}
			catch (Exception e) {
				LOGGER.debug("unable to encode uri path from context, falling back to uri from request", e);
			}
		}
		// remove double slashes
		uri = uri.replace("//", "/");
		return uri;
	}

	private static String characterEncoding(HttpServletRequest request) {
		return request.getCharacterEncoding() != null ? request.getCharacterEncoding()
				: WebUtils.DEFAULT_CHARACTER_ENCODING;
	}
}
