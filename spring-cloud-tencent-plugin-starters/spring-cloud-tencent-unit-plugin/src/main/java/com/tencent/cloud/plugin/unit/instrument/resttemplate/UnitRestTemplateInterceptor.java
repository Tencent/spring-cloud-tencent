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

package com.tencent.cloud.plugin.unit.instrument.resttemplate;

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.plugin.unit.utils.SpringCloudUnitUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.model.UnitArch;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Interceptor used for unit in RestTemplate.
 *
 * @author Shedfree Wu
 */
public class UnitRestTemplateInterceptor implements ClientHttpRequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.RestTemplate.UNIT_INTERCEPTOR_ORDER;
	}

	/**
	 * @see org.springframework.http.client.ClientHttpRequestInterceptor#intercept(org.springframework.http.HttpRequest, byte[], org.springframework.http.client.ClientHttpRequestExecution)
	 */
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return execution.execute(getHttpRequestWrapper(request), body);
	}

	public HttpRequest getHttpRequestWrapper(HttpRequest httpRequest) {
		// 未开启单元化, 直接返回
		if (!TencentUnitManager.isEnable()) {
			return httpRequest;
		}

		String serviceName = httpRequest.getURI().getHost();
		SpringCloudUnitUtils.preRequestRecordUnitContext(serviceName);
		// 不需要转发到网关, 直接返回
		if (!TencentUnitContext.containRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY)) {
			return httpRequest;
		}

		UnitArch.Gateway gateway = (UnitArch.Gateway) TencentUnitContext.getObjectRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY);
		URI unitRouteUri = UriComponentsBuilder.fromUri(httpRequest.getURI()).host(gateway.getServiceName()).build()
				.toUri();
		return new UriModifyHttpRequestWrapper(httpRequest, unitRouteUri);
	}
}
