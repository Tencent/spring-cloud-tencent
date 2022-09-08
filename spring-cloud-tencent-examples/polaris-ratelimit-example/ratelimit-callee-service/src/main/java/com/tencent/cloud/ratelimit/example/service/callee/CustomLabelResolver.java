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

package com.tencent.cloud.ratelimit.example.service.callee;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;

import org.springframework.stereotype.Component;

/**
 * resolver custom label from request.
 *
 * @author lepdou 2022-03-31
 */
@Component
public class CustomLabelResolver implements PolarisRateLimiterLabelServletResolver {

	@Override
	public Map<String, String> resolve(HttpServletRequest request) {
		// rate limit by some request params. such as query params, headers ..

		Map<String, String> labels = new HashMap<>();
		labels.put("user", "zhangsan");

		return labels;
	}
}
