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
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.pojo.PolarisServer;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.loadbalancer.LoadBalancerUtils;
import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import com.tencent.cloud.polaris.loadbalancer.PolarisRingHashRule;
import com.tencent.cloud.polaris.loadbalancer.PolarisWeightedRandomRule;
import com.tencent.cloud.polaris.loadbalancer.PolarisWeightedRoundRobinRule;
import com.tencent.cloud.polaris.loadbalancer.config.PolarisLoadBalancerProperties;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.cloud.polaris.router.spi.RouterResponseInterceptor;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import org.yaml.snakeyaml.util.UriEncoder;

import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Service routing entrance.
 * <p>
 * Rule routing needs to rely on request parameters for server filtering,
 * and {@link com.netflix.loadbalancer.ServerListFilter#getFilteredListOfServers(List)}
 * The interface cannot obtain the context object of the request granularity,
 * so the routing capability cannot be achieved through ServerListFilter.
 * <p>
 * And {@link com.netflix.loadbalancer.IRule#choose(Object)} provides the ability to pass in context parameters,
 * so routing capabilities are implemented through IRule.
 *
 * @author Haotian Zhang, lepdou
 */
public class PolarisLoadBalancerCompositeRule extends AbstractLoadBalancerRule {

	final static String STRATEGY_RANDOM = "random";
	final static String STRATEGY_ROUND_ROBIN = "roundRobin";
	final static String STRATEGY_WEIGHT = "polarisWeightedRandom";
	final static String STRATEGY_WEIGHT_ROUND_ROBIN = "polarisWeightedRoundRobin";
	final static String STRATEGY_HASH_RING = "polarisRingHash";
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

	private final AtomicBoolean initializedLoadBalancerToDelegateRule = new AtomicBoolean(false);

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
		ILoadBalancer loadBalancer = getLoadBalancer();

		if (!(loadBalancer instanceof PolarisLoadBalancer)) {
			return loadBalancer.chooseServer(key);
		}

		// set load balancer to delegate rule
		if (initializedLoadBalancerToDelegateRule.compareAndSet(false, true)) {
			delegateRule.setLoadBalancer(loadBalancer);
		}

		PolarisLoadBalancer polarisLoadBalancer = (PolarisLoadBalancer) loadBalancer;

		// 1. get all servers from polaris client
		List<Server> allServers = polarisLoadBalancer.getReachableServersWithoutCache();
		if (CollectionUtils.isEmpty(allServers)) {
			return null;
		}

		// 2. filter by router
		List<Server> serversAfterRouter;
		if (key != null) {
			if (key instanceof PolarisRouterContext) {
				// router implement for Feign and scg
				PolarisRouterContext routerContext = (PolarisRouterContext) key;
				serversAfterRouter = doRouter(allServers, routerContext);
			}
			else if (key instanceof HttpRequest) {
				// router implement for rest template
				HttpRequest request = (HttpRequest) key;

				String routerContextStr = request.getHeaders().getFirst(RouterConstant.HEADER_ROUTER_CONTEXT);

				if (StringUtils.isEmpty(routerContextStr)) {
					serversAfterRouter = allServers;
				}
				else {
					PolarisRouterContext routerContext = JacksonUtils.deserialize(UriEncoder.decode(routerContextStr),
							PolarisRouterContext.class);
					serversAfterRouter = doRouter(allServers, routerContext);
				}
			}
			else {
				serversAfterRouter = allServers;
			}
		}
		else {
			serversAfterRouter = allServers;
		}

		// 3. put filtered servers to thread local, so delegate rule choose servers from filtered servers.
		polarisLoadBalancer.addServers(serversAfterRouter);

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
			return new PolarisWeightedRandomRule(routerAPI);
		case STRATEGY_WEIGHT_ROUND_ROBIN:
			return new PolarisWeightedRoundRobinRule(routerAPI);
		case STRATEGY_HASH_RING:
			return new PolarisRingHashRule(routerAPI);
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
