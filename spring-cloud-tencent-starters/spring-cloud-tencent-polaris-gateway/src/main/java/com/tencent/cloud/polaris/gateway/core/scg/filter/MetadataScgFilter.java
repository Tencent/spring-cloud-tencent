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
import com.tencent.cloud.metadata.context.MetadataContext;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.metadata.util.JacksonUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Scg filter used for writing metadata in HTTP request header.
 *
 * @author Haotian Zhang
 */
public class MetadataScgFilter extends AbstractGlobalFilter {

    private static final int METADATA_SCG_FILTER_ORDER = 10151;

    @Override
    public int getOrder() {
        return METADATA_SCG_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return true;
    }

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // get request builder
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

        // get metadata of current thread
        MetadataContext metadataContext = exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);

        // add new metadata and cover old
        if (metadataContext == null) {
            metadataContext = MetadataContextHolder.get();
        }
        Map<String, String> customMetadata = metadataContext.getAllTransitiveCustomMetadata();
        if (!CollectionUtils.isEmpty(customMetadata)) {
            String metadataStr = JacksonUtils.serializeToJson(customMetadata);
            try {
                builder.header(MetadataConstant.HeaderName.CUSTOM_METADATA, URLEncoder.encode(metadataStr, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                builder.header(MetadataConstant.HeaderName.CUSTOM_METADATA, metadataStr);
            }
        }

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }
}
