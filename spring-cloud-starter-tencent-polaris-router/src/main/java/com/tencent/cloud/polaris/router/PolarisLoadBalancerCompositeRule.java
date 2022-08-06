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

package com.tencent.cloud.polaris.router;

import java.util.ArrayList;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.RetryRule;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.WeightedResponseTimeRule;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.polaris.loadbalancer.LoadBalancerUtils;
import com.tencent.cloud.polaris.loadbalancer.PolarisWeightedRule;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterResponseInterceptor;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 *
 * Service routing entrance.
 *
 * Rule routing needs to rely on request parameters for server filtering,
 * and {@link com.netflix.loadbalancer.ServerListFilter#getFilteredListOfServers(List)}
 * The interface cannot obtain the context object of the request granularity,
 * so the routing capability cannot be achieved through ServerListFilter.
 *
 * And {@link com.netflix.loadbalancer.IRule#choose(Object)} provides the ability to pass in context parameters,
 * so routing capabilities are implemented through IRule.
 *
 * @author Haotian Zhang, lepdou
 */
public class PolarisLoadBalancerCompositeRule extends AbstractLoadBalancerRule {

	final static String STRATEGY_RANDOM = "random";
	final static String STRATEGY_ROUND_ROBIN = "roundRobin";
	final static String STRATEGY_WEIGHT = "polarisWeighted";
	final static String STRATEGY_RETRY = "retry";
	final static String STRATEGY_RESPONSE_TIME_WEIGHTED = "responseTimeWeighted";
	final static String STRATEGY_BEST_AVAILABLE = "bestAvailable";
	final static String STRATEGY_ZONE_AVOIDANCE = "zoneAvoidance";
	final static String STRATEGY_AVAILABILITY_FILTERING = "availabilityFilteringRule";

	private final PolarisLoadBalancerProperties loadBalancerProperties;
	private final RouterAPI routerAPI;
	private final List<RouterRequestInterceptor> requestInterceptors;
	private final List<RouterResponseInterceptor> responseInterceptors;

	private final AbstractLoadBalancerRule delegateRule;

	public PolarisLoadBalancerCompositeRule(RouterAPI routerAPI,
			PolarisLoadBalancerProperties polarisLoadBalancerProperties,
			IClientConfig iClientConfig,
			List<RouterRequestInterceptor> requestInterceptors,
			List<RouterResponseInterceptor> responseInterceptors,
			AbstractLoadBalancerRule delegate) {
		this.routerAPI = routerAPI;
		this.loadBalancerProperties = polarisLoadBalancerProperties;
		this.requestInterceptors = requestInterceptors;
		this.responseInterceptors = responseInterceptors;

		AbstractLoadBalancerRule loadBalancerRule = getRule();
		if (loadBalancerRule != null) {
			delegateRule = loadBalancerRule;
			delegateRule.initWithNiwsConfig(iClientConfig);
		}
		else {
			delegateRule = delegate;
		}
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
	}

	@Override
	public Server choose(Object key) {
		// 1. get all servers
		List<Server> allServers = getLoadBalancer().getReachableServers();
		if (CollectionUtils.isEmpty(allServers)) {
			return null;
		}

		ILoadBalancer loadBalancer = new SimpleLoadBalancer();
		// 2. filter by router
		if (key instanceof PolarisRouterContext) {
			PolarisRouterContext routerContext = (PolarisRouterContext) key;
			List<Server> serversAfterRouter = doRouter(allServers, routerContext);
			// 3. filter by load balance.
			// A LoadBalancer needs to be regenerated for each request,
			// because the list of servers may be different after filtered by router
			loadBalancer.addServers(serversAfterRouter);
		}
		else {
			loadBalancer.addServers(allServers);
		}
		delegateRule.setLoadBalancer(loadBalancer);

		return delegateRule.choose(key);
	}

	List<Server> doRouter(List<Server> allServers, PolarisRouterContext routerContext) {
		ServiceInstances serviceInstances = LoadBalancerUtils.transferServersToServiceInstances(allServers);

		ProcessRoutersRequest processRoutersRequest = buildProcessRoutersBaseRequest(serviceInstances);

		// process request interceptors
		processRouterRequestInterceptors(processRoutersRequest, routerContext);

		// process router chain
		ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRoutersRequest);

		// process response interceptors
		processRouterResponseInterceptors(routerContext, processRoutersResponse);

		// transfer polaris server to ribbon server
		ServiceInstances filteredServiceInstances = processRoutersResponse.getServiceInstances();
		List<Server> filteredInstances = new ArrayList<>();
		for (Instance instance : filteredServiceInstances.getInstances()) {
			filteredInstances.add(new PolarisServer(serviceInstances, instance));
		}

		return filteredInstances;
	}

	ProcessRoutersRequest buildProcessRoutersBaseRequest(ServiceInstances serviceInstances) {
		ProcessRoutersRequest processRoutersRequest = new ProcessRoutersRequest();
		processRoutersRequest.setDstInstances(serviceInstances);
		ServiceInfo serviceInfo = new ServiceInfo();
		serviceInfo.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		serviceInfo.setService(MetadataContext.LOCAL_SERVICE);
		processRoutersRequest.setSourceService(serviceInfo);
		return processRoutersRequest;
	}

	void processRouterRequestInterceptors(ProcessRoutersRequest processRoutersRequest, PolarisRouterContext routerContext) {
		for (RouterRequestInterceptor requestInterceptor : requestInterceptors) {
			requestInterceptor.apply(processRoutersRequest, routerContext);
		}
	}

	private void processRouterResponseInterceptors(PolarisRouterContext routerContext, ProcessRoutersResponse processRoutersResponse) {
		if (!CollectionUtils.isEmpty(responseInterceptors)) {
			for (RouterResponseInterceptor responseInterceptor : responseInterceptors) {
				responseInterceptor.apply(processRoutersResponse, routerContext);
			}
		}
	}

	public AbstractLoadBalancerRule getRule() {
		String loadBalanceStrategy = loadBalancerProperties.getStrategy();
		if (StringUtils.isEmpty(loadBalanceStrategy)) {
			return null;
		}
		switch (loadBalanceStrategy) {
		case STRATEGY_RANDOM:
			return new RandomRule();
		case STRATEGY_WEIGHT:
			return new PolarisWeightedRule(routerAPI);
		case STRATEGY_RETRY:
			return new RetryRule();
		case STRATEGY_RESPONSE_TIME_WEIGHTED:
			return new WeightedResponseTimeRule();
		case STRATEGY_BEST_AVAILABLE:
			return new BestAvailableRule();
		case STRATEGY_ROUND_ROBIN:
			return new RoundRobinRule();
		case STRATEGY_AVAILABILITY_FILTERING:
			return new AvailabilityFilteringRule();
		case STRATEGY_ZONE_AVOIDANCE:
		default:
			return new ZoneAvoidanceRule();
		}
	}

	public AbstractLoadBalancerRule getDelegateRule() {
		return delegateRule;
	}
}
