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

package com.tencent.cloud.polaris.ratelimit.spi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve custom label from request. The label used for rate limit params.
 *
 * @author lepdou 2022-03-31
 */
public interface PolarisRateLimiterLabelServletResolver {

	/**
	 * Resolve custom label from request.
	 * @param request the http request
	 * @return resolved labels
	 */
	Map<String, String> resolve(HttpServletRequest request);
}
