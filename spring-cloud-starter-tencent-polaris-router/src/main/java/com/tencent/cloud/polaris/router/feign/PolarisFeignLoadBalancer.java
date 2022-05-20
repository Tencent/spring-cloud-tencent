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

package com.tencent.cloud.polaris.router.feign;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.tencent.cloud.common.util.ExpressionLabelUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterConstants;

import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.util.CollectionUtils;

/**
 * In order to pass router context for {@link com.tencent.cloud.polaris.router.PolarisLoadBalancerCompositeRule}.
 *
 *@author lepdou 2022-05-16
 */
public class PolarisFeignLoadBalancer extends FeignLoadBalancer {

	public PolarisFeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig, ServerIntrospector serverIntrospector) {
		super(lb, clientConfig, serverIntrospector);
	}

	@Override
	protected void customizeLoadBalancerCommandBuilder(RibbonRequest request, IClientConfig config,
			LoadBalancerCommand.Builder<RibbonResponse> builder) {
		Map<String, Collection<String>> headers = request.getRequest().headers();
		Collection<String> labelHeaderValues = headers.get(RouterConstants.ROUTER_LABEL_HEADER);

		if (CollectionUtils.isEmpty(labelHeaderValues)) {
			builder.withServerLocator(null);
			return;
		}

		PolarisRouterContext routerContext = new PolarisRouterContext();

		labelHeaderValues.forEach(labelHeaderValue -> {
			Map<String, String> labels = JacksonUtils.deserialize2Map(labelHeaderValue);
			if (!CollectionUtils.isEmpty(labels)) {
				Map<String, String> unescapeLabels = new HashMap<>(labels.size());
				for (Map.Entry<String, String> entry : labels.entrySet()) {
					String escapedKey = ExpressionLabelUtils.unescape(entry.getKey());
					String escapedValue = ExpressionLabelUtils.unescape(entry.getValue());
					unescapeLabels.put(escapedKey, escapedValue);
				}
				routerContext.setLabels(unescapeLabels);
			}
		});

		builder.withServerLocator(routerContext);
	}
}
