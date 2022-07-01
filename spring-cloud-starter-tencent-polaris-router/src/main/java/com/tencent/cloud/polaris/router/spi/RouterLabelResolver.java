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

package com.tencent.cloud.polaris.router.spi;

import java.util.Collections;
import java.util.Map;

import feign.RequestTemplate;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;

/**
 * The spi for resolving labels from request.
 *
 * @author lepdou 2022-05-11
 */
public interface RouterLabelResolver extends Ordered {

	/**
	 * resolve labels from feign request.
	 * @param requestTemplate the feign request.
	 * @return resolved labels
	 */
	default Map<String, String> resolve(RequestTemplate requestTemplate) {
		return Collections.emptyMap();
	}

	/**
	 * resolve labels from rest template request.
	 * @param request the rest template request.
	 * @param body the rest template request body.
	 * @return resolved labels
	 */
	default Map<String, String> resolve(HttpRequest request, byte[] body) {
		return Collections.emptyMap();
	}
}
