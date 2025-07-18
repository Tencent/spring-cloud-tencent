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

package com.tencent.cloud.polaris.router.instrument.scg;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.metadata.provider.ReactiveMetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.filter.LoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;

/**
 * Interceptor used for setting SCG ServerWebExchange metadata provider.
 *
 * @author Hoatian Zhang
 */
public class RouterLabelGlobalFilter implements GlobalFilter, Ordered {
	@Override
	public int getOrder() {
		return LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
	}

	/**
	 * Copied from ReactiveLoadBalancerClientFilter, and create new RequestData for passing router labels.
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.MESSAGE, false)
				.setMetadataProvider(new ReactiveMetadataProvider(exchange.getRequest(),
						CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
								.getRawMetadataStringValue(MetadataConstants.LOCAL_IP)));
		return chain.filter(exchange);
	}
}
