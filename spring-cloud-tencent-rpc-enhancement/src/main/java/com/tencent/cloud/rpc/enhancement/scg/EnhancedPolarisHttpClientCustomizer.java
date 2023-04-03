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

package com.tencent.cloud.rpc.enhancement.scg;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.client.api.SDKContext;
import reactor.netty.http.client.HttpClient;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;

public class EnhancedPolarisHttpClientCustomizer implements HttpClientCustomizer {

	private final RpcEnhancementReporterProperties properties;
	private final SDKContext context;
	private final ConsumerAPI consumerAPI;

	public EnhancedPolarisHttpClientCustomizer(RpcEnhancementReporterProperties properties, SDKContext context, ConsumerAPI consumerAPI) {
		this.properties = properties;
		this.context = context;
		this.consumerAPI = consumerAPI;
	}

	@Override
	public HttpClient customize(HttpClient httpClient) {
		return new EnhancedPolarisHttpClient(httpClient, properties, context, consumerAPI);
	}
}
