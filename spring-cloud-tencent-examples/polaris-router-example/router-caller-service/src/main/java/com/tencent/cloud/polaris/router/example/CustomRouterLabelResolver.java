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

package com.tencent.cloud.polaris.router.example;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.tencent.cloud.polaris.router.spi.RouterLabelResolver;
import feign.RequestTemplate;

import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Component;

/**
 *
 * Customize the business tag information obtained from the request
 *
 *@author lepdou 2022-05-12
 */
@Component
public class CustomRouterLabelResolver implements RouterLabelResolver {
	private final Gson gson = new Gson();

	@Override
	public Map<String, String> resolve(RequestTemplate requestTemplate) {
		Map<String, String> labels = new HashMap<>();

		User user = gson.fromJson(new String(requestTemplate.body()), User.class);

		labels.put("user", user.getName());

		return labels;
	}

	@Override
	public Map<String, String> resolve(HttpRequest request, byte[] body) {
		Map<String, String> labels = new HashMap<>();
		User user = gson.fromJson(new String(body), User.class);

		labels.put("user", user.getName());
		return labels;
	}


	@Override
	public int getOrder() {
		return 0;
	}
}
