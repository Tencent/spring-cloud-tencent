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

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.IOException;
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

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor capabilities.
 * Parses the label from the request and puts it into the RouterContext for routing.
 *
 * @author lepdou 2022-05-18
 */
public class PolarisLoadBalancerInterceptor extends LoadBalancerInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisLoadBalancerInterceptor.class);

	private final LoadBalancerClient loadBalancer;
	private final LoadBalancerRequestFactory requestFactory;
	private final List<SpringWebRouterLabelResolver> routerLabelResolvers;
	private final StaticMetadataManager staticMetadataManager;
	private final RouterRuleLabelResolver routerRuleLabelResolver;

	private final boolean isRibbonLoadBalanceClient;

	public PolarisLoadBalancerInterceptor(LoadBalancerClient loadBalancer,
			LoadBalancerRequestFactory requestFactory,
			List<SpringWebRouterLabelResolver> routerLabelResolvers,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		super(loadBalancer, requestFactory);
		this.loadBalancer = loadBalancer;
		this.requestFactory = requestFactory;
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
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		String peerServiceName = originalUri.getHost();
		Assert.state(peerServiceName != null,
				"Request URI does not contain a valid hostname: " + originalUri);

		if (isRibbonLoadBalanceClient) {
			PolarisRouterContext routerContext = genRouterContext(request, body, peerServiceName);

			return ((RibbonLoadBalancerClient) loadBalancer).execute(peerServiceName,
					this.requestFactory.createRequest(request, body, execution), routerContext);
		}

		return this.loadBalancer.execute(peerServiceName,
				this.requestFactory.createRequest(request, body, execution));
	}

	PolarisRouterContext genRouterContext(HttpRequest request, byte[] body, String peerServiceName) {
		// local service labels
		Map<String, String> labels = new HashMap<>(staticMetadataManager.getMergedStaticMetadata());

		// labels from rule expression
		Set<String> expressionLabelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerServiceName);
		Map<String, String> ruleExpressionLabels = getExpressionLabels(request, expressionLabelKeys);
		if (!CollectionUtils.isEmpty(ruleExpressionLabels)) {
			labels.putAll(ruleExpressionLabels);
		}

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(request, body, expressionLabelKeys);
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

	private Map<String, String> getExpressionLabels(HttpRequest request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return SpringWebExpressionLabelUtils.resolve(request, labelKeys);
	}
}
