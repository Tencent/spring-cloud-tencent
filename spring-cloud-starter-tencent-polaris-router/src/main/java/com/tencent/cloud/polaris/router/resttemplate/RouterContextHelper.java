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

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Set router context to request and response.
 *
 * @author lepdou 2022-10-09
 */
public final class RouterContextHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RouterContextHelper.class);

	private RouterContextHelper() {

	}

	public static void setRouterContextToRequest(HttpRequest request, PolarisRouterContext routerContext) {
		try {
			request.getHeaders().add(RouterConstant.HEADER_ROUTER_CONTEXT,
					URLEncoder.encode(JacksonUtils.serialize2Json(routerContext), UTF_8));
		}
		catch (Exception e) {
			LOGGER.error("[SCT] serialize router context error.", e);
		}
	}

	public static void setRouterContextToResponse(PolarisRouterContext routerContext, ClientHttpResponse response) {
		Map<String, String> labels = routerContext.getLabels(RouterConstant.ROUTER_LABELS);

		try {
			response.getHeaders().add(RouterConstant.ROUTER_LABELS,
					URLEncoder.encode(JacksonUtils.serialize2Json(labels), UTF_8));
		}
		catch (UnsupportedEncodingException e) {
			LOGGER.error("[SCT] add router label to response header error.", e);
		}
	}
}
