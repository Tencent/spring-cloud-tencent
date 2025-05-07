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

package com.tencent.cloud.polaris.router.instrument.feign;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.metadata.provider.FeignRequestTemplateMetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.core.Ordered;

/**
 * Interceptor used for setting Feign RequestTemplate metadata provider.
 *
 * @author lepdou, cheese8, Hoatian Zhang
 */
public class RouterLabelFeignInterceptor implements RequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.Feign.ROUTER_LABEL_INTERCEPTOR_ORDER;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.MESSAGE, false)
				.setMetadataProvider(new FeignRequestTemplateMetadataProvider(requestTemplate));
	}
}
