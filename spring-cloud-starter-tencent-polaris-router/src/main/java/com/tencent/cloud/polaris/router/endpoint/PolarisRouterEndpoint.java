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

package com.tencent.cloud.polaris.router.endpoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Router actuator endpoint.
 *
 * @author lepdou 2022-07-25
 */
@Endpoint(id = "polaris-router")
public class PolarisRouterEndpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRouterEndpoint.class);

	private final ServiceRuleManager serviceRuleManager;

	public PolarisRouterEndpoint(ServiceRuleManager serviceRuleManager) {
		this.serviceRuleManager = serviceRuleManager;
	}

	@ReadOperation
	public Map<String, Object> router(@Selector String dstService) {
		Map<String, Object> result = new HashMap<>();

		if (StringUtils.hasText(dstService)) {
			List<RoutingProto.Route> routerRules = serviceRuleManager.getServiceRouterRule(MetadataContext.LOCAL_NAMESPACE,
					MetadataContext.LOCAL_SERVICE, dstService);
			List<Object> rules = new LinkedList<>();
			result.put("routerRules", rules);

			if (CollectionUtils.isEmpty(routerRules)) {
				return result;
			}

			for (RoutingProto.Route route : routerRules) {
				rules.add(parseRouterRule(route));
			}
		}

		return result;
	}

	private Object parseRouterRule(RoutingProto.Route routeRule) {
		Map<String, Object> result = new HashMap<>();

		List<RoutingProto.Source> sourcePbs = routeRule.getSourcesList();
		List<Object> sources = new LinkedList<>();
		for (RoutingProto.Source sourcePb : sourcePbs) {
			sources.add(pb2Json(sourcePb));
		}
		result.put("sources", sources);

		List<RoutingProto.Destination> destPbs = routeRule.getDestinationsList();
		List<Object> destinations = new LinkedList<>();
		for (RoutingProto.Destination destPb : destPbs) {
			destinations.add(pb2Json(destPb));
		}
		result.put("destinations", destinations);

		return result;
	}

	private Object pb2Json(MessageOrBuilder pbObject) {
		String jsonStr;
		try {
			jsonStr = JsonFormat.printer().print(pbObject);
		}
		catch (InvalidProtocolBufferException e) {
			String msg = "parse router rule to json error.";
			LOGGER.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		return JacksonUtils.deserialize2Map(jsonStr);
	}
}
