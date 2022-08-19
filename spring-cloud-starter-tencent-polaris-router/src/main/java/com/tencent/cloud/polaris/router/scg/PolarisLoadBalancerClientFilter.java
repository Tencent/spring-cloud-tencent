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

package com.tencent.cloud.polaris.router.scg;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * Replaces the default LoadBalancerClientFilter implementation.
 *
 * @author lepdou 2022-06-10
 */
public class PolarisLoadBalancerClientFilter extends LoadBalancerClientFilter {
	private final static Logger LOGGER = LoggerFactory.getLogger(PolarisLoadBalancerClientFilter.class);

	private final StaticMetadataManager staticMetadataManager;
	private final RouterRuleLabelResolver routerRuleLabelResolver;
	private final List<SpringWebRouterLabelResolver> routerLabelResolvers;

	private final boolean isRibbonLoadBalanceClient;

	public PolarisLoadBalancerClientFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver,
			List<SpringWebRouterLabelResolver> routerLabelResolvers) {
		super(loadBalancer, properties);
		this.staticMetadataManager = staticMetadataManager;
		this.routerRuleLabelResolver = routerRuleLabelResolver;

		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.sort(Comparator.comparingInt(Ordered::getOrder));
			this.routerLabelResolvers = routerLabelResolvers;
		}
		else {
			this.routerLabelResolvers = null;
		}

		this.isRibbonLoadBalanceClient = loadBalancer instanceof RibbonLoadBalancerClient;
	}

	@Override
	protected ServiceInstance choose(ServerWebExchange exchange) {
		String peerServiceName = ((URI) exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR)).getHost();

		if (isRibbonLoadBalanceClient) {
			// Pass routing context to ribbon load balancer
			PolarisRouterContext routerContext = genRouterContext(exchange, peerServiceName);
			return ((RibbonLoadBalancerClient) loadBalancer).choose(peerServiceName, routerContext);
		}
		else {
			return loadBalancer.choose(peerServiceName);
		}
	}

	PolarisRouterContext genRouterContext(ServerWebExchange exchange, String peerServiceName) {
		// local service labels
		Map<String, String> labels = new HashMap<>(staticMetadataManager.getMergedStaticMetadata());

		// labels from rule expression
		Set<String> expressionLabelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerServiceName);
		Map<String, String> ruleExpressionLabels = getExpressionLabels(exchange, expressionLabelKeys);
		if (!CollectionUtils.isEmpty(ruleExpressionLabels)) {
			labels.putAll(ruleExpressionLabels);
		}

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(exchange, expressionLabelKeys);
					if (!CollectionUtils.isEmpty(customResolvedLabels)) {
						labels.putAll(customResolvedLabels);
					}
				}
				catch (Throwable t) {
					LOGGER.error("[SCT][Router] revoke RouterLabelResolver occur some exception. ", t);
				}
			});
		}

		// labels from downstream
		Map<String, String> transitiveLabels = MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		labels.putAll(transitiveLabels);

		PolarisRouterContext routerContext = new PolarisRouterContext();

		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);
		routerContext.putLabels(PolarisRouterContext.TRANSITIVE_LABELS, transitiveLabels);

		return routerContext;
	}

	private Map<String, String> getExpressionLabels(ServerWebExchange exchange, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return SpringWebExpressionLabelUtils.resolve(exchange, labelKeys);
	}
}
