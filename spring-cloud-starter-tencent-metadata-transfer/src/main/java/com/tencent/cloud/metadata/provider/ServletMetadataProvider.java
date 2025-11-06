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

package com.tencent.cloud.metadata.provider;

import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.ServletExpressionLabelUtils;
import com.tencent.cloud.metadata.pojo.SnapshotHttpServletRequest;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;
import jakarta.servlet.http.HttpServletRequest;

/**
 * MetadataProvider used for Servlet.
 *
 * @author Shedfree Wu
 */
public class ServletMetadataProvider implements MetadataProvider {

	private final HttpServletRequest httpServletRequest;

	private final String callerIp;

	public ServletMetadataProvider(HttpServletRequest httpServletRequest, String callerIp) {
		this(httpServletRequest, callerIp, false);
	}

	public ServletMetadataProvider(HttpServletRequest httpServletRequest, String callerIp, boolean isAsync) {
		if (isAsync) {
			this.httpServletRequest = SnapshotHttpServletRequest.from(httpServletRequest);
		}
		else {
			this.httpServletRequest = httpServletRequest;
		}
		this.callerIp = callerIp;
	}

	@Override
	public String getRawMetadataStringValue(String key) {
		switch (key) {
		case MessageMetadataContainer.LABEL_KEY_METHOD:
			return httpServletRequest.getMethod();
		case MessageMetadataContainer.LABEL_KEY_PATH:
			return UrlUtils.decode(httpServletRequest.getRequestURI());
		case MessageMetadataContainer.LABEL_KEY_CALLER_IP:
			return callerIp;
		default:
			return null;
		}
	}

	@Override
	public String getRawMetadataMapValue(String key, String mapKey) {
		switch (key) {
		case MessageMetadataContainer.LABEL_MAP_KEY_HEADER:
			return UrlUtils.decode(httpServletRequest.getHeader(mapKey));
		case MessageMetadataContainer.LABEL_MAP_KEY_COOKIE:
			return UrlUtils.decode(ServletExpressionLabelUtils.getCookieValue(httpServletRequest.getCookies(), mapKey, null));
		case MessageMetadataContainer.LABEL_MAP_KEY_QUERY:
			return UrlUtils.decode(ExpressionLabelUtils.getQueryValue(httpServletRequest.getQueryString(), mapKey, null));
		default:
			return null;
		}
	}

	@JustForTest
	HttpServletRequest getHttpServletRequest() {
		return httpServletRequest;
	}
}
