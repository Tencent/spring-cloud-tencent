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

package com.tencent.cloud.metadata.provider;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.ServletExpressionLabelUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;

/**
 * MetadataProvider used for Servlet.
 *
 * @author Shedfree Wu
 */
public class ServletMetadataProvider implements MetadataProvider {

	private HttpServletRequest httpServletRequest;

	public ServletMetadataProvider(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	@Override
	public String getRawMetadataStringValue(String key) {
		switch (key) {
			case MessageMetadataContainer.LABEL_KEY_METHOD:
				return httpServletRequest.getMethod();
			case MessageMetadataContainer.LABEL_KEY_PATH:
				return httpServletRequest.getRequestURI();
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
				return ServletExpressionLabelUtils.getCookieValue(httpServletRequest.getCookies(), mapKey, null);
			case MessageMetadataContainer.LABEL_MAP_KEY_QUERY:
				return ExpressionLabelUtils.getQueryValue(httpServletRequest.getQueryString(), mapKey, null);
			default:
				return null;
		}
	}
}
