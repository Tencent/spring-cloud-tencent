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

package com.tencent.cloud.plugin.gateway.context;

import java.util.List;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.constant.GatewayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;


public final class PathRewriteUtil {

	private static final Logger logger = LoggerFactory.getLogger(PathRewriteUtil.class);
	private static final String BLOCKED = "Y";

	private PathRewriteUtil() {

	}

	public static String getNewPath(List<PathRewrite> pathRewrites, String path, ServerWebExchange exchange) {
		String newPath = path;
		for (PathRewrite pathRewrite : pathRewrites) {
			String regex = pathRewrite.getRegex();
			String replacement = pathRewrite.getReplacement();
			String blockedRegex = replacement.replaceAll("\\$(\\d+|\\{[^/]+?\\})", ".*");

			if (!StringUtils.isBlank(blockedRegex) && BLOCKED.equalsIgnoreCase(pathRewrite.getBlocked())
					&& path.matches(blockedRegex)) {
				logger.warn("[TSF-MSGW] uri is blocked by rule. uri: [{}] rule: [{}] -> [{}]",
						path, regex, replacement);
				ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "uri is blocked by rule. uri: [" + path + "] rule: [" + regex + "] -> [" + replacement + "]");
				exchange.getAttributes().put(GatewayConstant.PATH_REWRITE_EXCEPTION, exception);
				break;
			}

			if (!StringUtils.isBlank(regex) && !StringUtils.isBlank(replacement) && path.matches(regex)) {
				newPath = path.replaceAll(regex, replacement);
				logger.info("[TSF-MSGW] rewrite path [{}] to [{}], rule: [{}] -> [{}]", path, newPath, regex, replacement);
				break;
			}
		}
		return newPath;
	}
}
