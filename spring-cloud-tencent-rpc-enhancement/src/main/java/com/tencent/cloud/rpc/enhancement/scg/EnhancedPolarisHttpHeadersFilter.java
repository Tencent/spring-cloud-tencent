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

import java.util.List;

import com.tencent.cloud.common.constant.HeaderConstant;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;

public class EnhancedPolarisHttpHeadersFilter implements HttpHeadersFilter {

	public EnhancedPolarisHttpHeadersFilter() {
	}

	@Override
	public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
		Response<ServiceInstance> serviceInstanceResponse = exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
		if (serviceInstanceResponse == null || !serviceInstanceResponse.hasServer()) {
			return input;
		}
		ServiceInstance instance = serviceInstanceResponse.getServer();
		write(input, HeaderConstant.INTERNAL_CALLEE_SERVICE_ID, instance.getServiceId(), true);
		write(input, HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST, instance.getHost(), true);
		write(input, HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT, instance.getPort() + "", true);
		return input;
	}

	@Override
	public boolean supports(Type type) {
		return Type.REQUEST.equals(type);
	}

	private void write(HttpHeaders headers, String name, String value, boolean append) {
		if (value == null) {
			return;
		}
		if (append) {
			headers.add(name, value);
			// these headers should be treated as a single comma separated header
			List<String> values = headers.get(name);
			String delimitedValue = StringUtils.collectionToCommaDelimitedString(values);
			headers.set(name, delimitedValue);
		}
		else {
			headers.set(name, value);
		}
	}
}
