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

package com.tencent.cloud.polaris.gateway.core.scg.filter;

import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.polaris.gateway.core.route.DynamicRouteService;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filter for implement dynamic route.
 *
 * @author kan peng
 */
public class DynamicRouteScgFilter implements WebFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicRouteScgFilter.class);

    @Autowired
    private DynamicRouteService dynamicRouteService;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Override
    public int getOrder() {
        return MetadataConstant.OrderConstant.FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {


        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        String requestPath = serverHttpRequest.getURI().getPath();
        String[] pathList = requestPath.split("/");
        String id = pathList[1];
        String path = String.format("/%s/**",id);

        //Check whether the route has been loaded
        for(RouteDefinition routeDefinition: gatewayProperties.getRoutes()){
            if (id.equals(routeDefinition.getId())){
                return webFilterChain.filter(serverWebExchange)
                        .doOnError(throwable -> LOG.error("handle DynamicRouteFilter[{}] error.", MetadataContextHolder.get(), throwable))
                        .doFinally((type) -> MetadataContextHolder.remove());
            }
        }

        RouteDefinition definition = new RouteDefinition();
        definition.setId(id);
        URI uri = URI.create("lb://"+id);
        definition.setUri(uri);

        //Define the first assertion
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");

        Map<String, String> predicateParams = new HashMap<>(8);
        predicateParams.put("pattern", path);
        predicate.setArgs(predicateParams);

        //Define Filter
        FilterDefinition filter = new FilterDefinition();
        filter.setName("StripPrefix");
        filter.addArg("parts","1");

        definition.setFilters(Arrays.asList(filter));
        definition.setPredicates(Arrays.asList(predicate));
        dynamicRouteService.add(definition);

        return webFilterChain.filter(serverWebExchange)
                .doOnError(throwable -> LOG.error("handle DynamicRouteFilter[{}] error.", MetadataContextHolder.get(), throwable))
                .doFinally((type) -> MetadataContextHolder.remove());
    }

}