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

package com.tencent.cloud.polaris.contract.filter;

import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_RESOURCE_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_UI_V2_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_UI_V3_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V2_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V3_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V2_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V3_PREFIX;

/**
 * Filter to disable api doc controller.
 *
 * @author Haotian Zhang
 */
public class ApiDocWebFluxFilter implements WebFilter {

	private final PolarisContractProperties polarisContractProperties;

	public ApiDocWebFluxFilter(PolarisContractProperties polarisContractProperties) {
		this.polarisContractProperties = polarisContractProperties;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {
		if (!polarisContractProperties.isExposure()) {
			String path = serverWebExchange.getRequest().getURI().getPath();
			if (path.startsWith(SWAGGER_V2_API_DOC_URL) ||
					path.startsWith(SWAGGER_V3_API_DOC_URL) ||
					path.startsWith(SWAGGER_UI_V2_URL) ||
					path.startsWith(SWAGGER_UI_V3_URL) ||
					path.startsWith(SWAGGER_RESOURCE_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V2_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V3_PREFIX)) {
				ServerHttpResponse response = serverWebExchange.getResponse();
				response.setRawStatusCode(HttpStatus.FORBIDDEN.value());
				DataBuffer dataBuffer = response.bufferFactory().allocateBuffer();
				return response.writeWith(Mono.just(dataBuffer));
			}
		}
		return webFilterChain.filter(serverWebExchange);
	}
}

