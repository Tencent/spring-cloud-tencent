/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.router.instrument.resttemplate;

import java.io.IOException;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.metadata.provider.RestTemplateMetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;

import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

/**
 * Interceptor used for setting RestTemplate HttpRequest metadata provider.
 *
 * @author liuye, Hoatian Zhang
 */
public class RouterLabelRestTemplateInterceptor implements ClientHttpRequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.RestTemplate.ROUTER_LABEL_INTERCEPTOR_ORDER;
	}

	@Override
	public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
			@NonNull ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.MESSAGE, false)
				.setMetadataProvider(new RestTemplateMetadataProvider(request));
		return clientHttpRequestExecution.execute(request, body);
	}
}
