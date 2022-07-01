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

package com.tencent.cloud.polaris.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pb.RateLimitProto;
import com.tencent.polaris.client.pb.RoutingProto;

/**
 * the manager of service governance rules. for example: rate limit rule, router rules.
 *
 * @author lepdou 2022-05-13
 */
public class ServiceRuleManager {

	private final SDKContext sdkContext;

	private final FlowControlParam controlParam;

	public ServiceRuleManager(SDKContext sdkContext) {
		this.sdkContext = sdkContext;
		controlParam = new DefaultFlowControlParam();
		controlParam.setTimeoutMs(sdkContext.getConfig().getGlobal().getAPI().getTimeout());
		controlParam.setMaxRetry(sdkContext.getConfig().getGlobal().getAPI().getMaxRetryTimes());
		controlParam.setRetryIntervalMs(sdkContext.getConfig().getGlobal().getAPI().getRetryInterval());
	}

	public RateLimitProto.RateLimit getServiceRateLimitRule(String namespace, String service) {
		ServiceEventKey serviceEventKey = new ServiceEventKey(new ServiceKey(namespace, service),
				ServiceEventKey.EventType.RATE_LIMITING);

		DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
		svcKeysProvider.setSvcEventKey(serviceEventKey);

		ResourcesResponse resourcesResponse = BaseFlow
				.syncGetResources(sdkContext.getExtensions(), true, svcKeysProvider, controlParam);

		ServiceRule serviceRule = resourcesResponse.getServiceRule(serviceEventKey);
		if (serviceRule != null) {
			Object rule = serviceRule.getRule();
			if (rule instanceof RateLimitProto.RateLimit) {
				return (RateLimitProto.RateLimit) rule;
			}
		}

		return null;
	}

	public List<RoutingProto.Route> getServiceRouterRule(String namespace, String sourceService, String dstService) {
		Set<ServiceEventKey> routerKeys = new HashSet<>();

		ServiceEventKey dstSvcEventKey = new ServiceEventKey(new ServiceKey(namespace, dstService),
				ServiceEventKey.EventType.ROUTING);
		routerKeys.add(dstSvcEventKey);

		ServiceEventKey srcSvcEventKey = new ServiceEventKey(new ServiceKey(namespace, sourceService),
				ServiceEventKey.EventType.ROUTING);
		routerKeys.add(srcSvcEventKey);

		DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
		svcKeysProvider.setSvcEventKeys(routerKeys);


		ResourcesResponse resourcesResponse = BaseFlow
				.syncGetResources(sdkContext.getExtensions(), true, svcKeysProvider, controlParam);

		List<RoutingProto.Route> rules = new ArrayList<>();

		//get source service outbound rules.
		ServiceRule sourceServiceRule = resourcesResponse.getServiceRule(srcSvcEventKey);
		if (sourceServiceRule != null) {
			Object rule = sourceServiceRule.getRule();
			if (rule instanceof RoutingProto.Routing) {
				rules.addAll(((RoutingProto.Routing) rule).getOutboundsList());
			}
		}

		//get peer service inbound rules.
		ServiceRule dstServiceRule = resourcesResponse.getServiceRule(dstSvcEventKey);
		if (dstServiceRule != null) {
			Object rule = dstServiceRule.getRule();
			if (rule instanceof RoutingProto.Routing) {
				rules.addAll(((RoutingProto.Routing) rule).getInboundsList());
			}
		}

		return rules;
	}
}
