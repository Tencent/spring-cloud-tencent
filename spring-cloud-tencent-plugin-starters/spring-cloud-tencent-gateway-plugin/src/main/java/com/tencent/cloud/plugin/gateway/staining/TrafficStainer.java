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

package com.tencent.cloud.plugin.gateway.staining;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

/**
 * Staining according to request parameters. for example, when the request parameter uid=0, staining env=blue.
 * @author lepdou 2022-07-06
 */
public interface TrafficStainer extends Ordered {

	/**
	 * get stained labels from request.
	 * @param exchange the request.
	 * @return stained labels.
	 */
	Map<String, String> apply(ServerWebExchange exchange);
}
