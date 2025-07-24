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

package com.tencent.cloud.plugin.unit.instrument.feign;

import java.net.URI;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.plugin.unit.utils.SpringCloudUnitUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.model.UnitArch;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.core.Ordered;

/**
 * Interceptor used for setting Feign RequestTemplate metadata provider.
 *
 * @author lepdou, Hoatian Zhang
 */
public class UnitFeignRequestInterceptor implements RequestInterceptor, Ordered {

	@Override
	public int getOrder() {
		return OrderConstant.Client.Feign.UNIT_INTERCEPTOR_ORDER;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		// 开启单元化
		if (TencentUnitManager.isEnable()) {
			String httpServiceName = requestTemplate.feignTarget().url();
			URI uri = URI.create(httpServiceName);
			// 截取http://provider-demo 为 provider-demo,
			String serviceName = uri.getHost();

			SpringCloudUnitUtils.preRequestRecordUnitContext(serviceName);

			if (TencentUnitContext.containRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY)) {
				UnitArch.Gateway gateway = (UnitArch.Gateway) TencentUnitContext.getObjectRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY);
				requestTemplate.target(uri.getScheme() + "//" + gateway.getServiceName());
			}
		}
	}
}
