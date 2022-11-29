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

package com.tencent.cloud.polaris.router.interceptor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.config.properties.PolarisRuleBasedRouterProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.plugins.router.rule.RuleBasedRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

import org.springframework.util.CollectionUtils;

/**
 * Router request interceptor for rule based router.
 * @author lepdou, Hoatian Zhang
 */
public class RuleBasedRouterRequestInterceptor implements RouterRequestInterceptor {

	private final PolarisRuleBasedRouterProperties polarisRuleBasedRouterProperties;

	public RuleBasedRouterRequestInterceptor(PolarisRuleBasedRouterProperties polarisRuleBasedRouterProperties) {
		this.polarisRuleBasedRouterProperties = polarisRuleBasedRouterProperties;
	}

	@Override
	public void apply(ProcessRoutersRequest request, PolarisRouterContext routerContext) {
		boolean ruleBasedRouterEnabled = polarisRuleBasedRouterProperties.isEnabled();

		// set dynamic switch for rule based router
		Set<RouteArgument> routeArguments = new HashSet<>();
		routeArguments.add(RouteArgument.buildCustom(RuleBasedRouter.ROUTER_ENABLED, String.valueOf(ruleBasedRouterEnabled)));

		// The label information that the rule based routing depends on
		// is placed in the metadata of the source service for transmission.
		// Later, can consider putting it in routerMetadata like other routers.
		if (ruleBasedRouterEnabled) {
			Map<String, String> ruleRouterLabels = routerContext.getLabels(RouterConstant.ROUTER_LABELS);
			if (!CollectionUtils.isEmpty(ruleRouterLabels)) {
				for (Map.Entry<String, String> label : ruleRouterLabels.entrySet()) {
					routeArguments.add(RouteArgument.fromLabel(label.getKey(), label.getValue()));
				}
			}
		}

		request.putRouterArgument(RuleBasedRouter.ROUTER_TYPE_RULE_BASED, routeArguments);
	}
}
