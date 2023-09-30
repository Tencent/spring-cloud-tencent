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
import java.util.List;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the manager of service governance rules. for example: rate limit rule, router rules, circuit breaker rules.
 *
 * @author lepdou 2022-05-13
 */
public class ServiceRuleManager {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceRuleManager.class);
	private final SDKContext sdkContext;
	private final ConsumerAPI consumerAPI;

	public ServiceRuleManager(SDKContext sdkContext, ConsumerAPI consumerAPI) {
		this.sdkContext = sdkContext;
		this.consumerAPI = consumerAPI;
	}

	public RateLimitProto.RateLimit getServiceRateLimitRule(String namespace, String service) {
		LOG.debug("Get service rate limit rules with namespace:{} and service:{}.", namespace, service);

		ServiceRule serviceRule = getServiceRule(namespace, service, ServiceEventKey.EventType.RATE_LIMITING);
		if (serviceRule != null) {
			Object rule = serviceRule.getRule();
			if (rule instanceof RateLimitProto.RateLimit) {
				return (RateLimitProto.RateLimit) rule;
			}
		}

		return null;
	}

	public List<RoutingProto.Route> getServiceRouterRule(String namespace, String sourceService, String dstService) {
		LOG.debug("Get service router rules with namespace:{} and sourceService:{} and dstService:{}.", namespace, sourceService, dstService);

		List<RoutingProto.Route> rules = new ArrayList<>();

		//get source service outbound rules.
		ServiceRule sourceServiceRule = getServiceRule(namespace, sourceService, ServiceEventKey.EventType.ROUTING);
		if (sourceServiceRule != null) {
			Object rule = sourceServiceRule.getRule();
			if (rule instanceof RoutingProto.Routing) {
				rules.addAll(((RoutingProto.Routing) rule).getOutboundsList());
			}
		}

		//get peer service inbound rules.
		ServiceRule dstServiceRule = getServiceRule(namespace, dstService, ServiceEventKey.EventType.ROUTING);
		if (dstServiceRule != null) {
			Object rule = dstServiceRule.getRule();
			if (rule instanceof RoutingProto.Routing) {
				rules.addAll(((RoutingProto.Routing) rule).getInboundsList());
			}
		}

		return rules;
	}

	public List<CircuitBreakerProto.CircuitBreakerRule> getServiceCircuitBreakerRule(String namespace, String sourceService, String dstService) {
		LOG.debug("Get service circuit breaker rules with namespace:{} and sourceService:{} and dstService:{}.", namespace, sourceService, dstService);

		List<CircuitBreakerProto.CircuitBreakerRule> rules = new ArrayList<>();

		// get source service circuit breaker rules.
		ServiceRule sourceServiceRule = getServiceRule(namespace, sourceService, ServiceEventKey.EventType.CIRCUIT_BREAKING);
		if (sourceServiceRule != null) {
			Object rule = sourceServiceRule.getRule();
			if (rule instanceof CircuitBreakerProto.CircuitBreaker) {
				rules.addAll(((CircuitBreakerProto.CircuitBreaker) rule).getRulesList());
			}
		}

		// get peer service circuit breaker rules.
		ServiceRule dstServiceRule = getServiceRule(namespace, dstService, ServiceEventKey.EventType.CIRCUIT_BREAKING);
		if (dstServiceRule != null) {
			Object rule = dstServiceRule.getRule();
			if (rule instanceof CircuitBreakerProto.CircuitBreaker) {
				rules.addAll(((CircuitBreakerProto.CircuitBreaker) rule).getRulesList());
			}
		}

		return rules;
	}

	private ServiceRule getServiceRule(String namespace, String service, ServiceEventKey.EventType eventType) {
		GetServiceRuleRequest getServiceRuleRequest = new GetServiceRuleRequest();
		getServiceRuleRequest.setRuleType(eventType);
		getServiceRuleRequest.setService(service);
		getServiceRuleRequest.setTimeoutMs(sdkContext.getConfig().getGlobal().getAPI().getTimeout());
		getServiceRuleRequest.setNamespace(namespace);

		ServiceRuleResponse res = consumerAPI.getServiceRule(getServiceRuleRequest);
		return res.getServiceRule();
	}

}
