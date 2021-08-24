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

package com.tencent.cloud.polaris.gateway.core.scg.filter;

import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.context.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.utils.Consts;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.tencent.cloud.polaris.gateway.core.scg.filter.MetadataFirstScgFilter.METADATA_FIRST_FILTER_ORDER;

/**
 * Scg filter used for writing metadata in HTTP request header.
 *
 * @author Haotian Zhang
 */
public class RateLimitScgFilter extends AbstractGlobalFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitScgFilter.class);

    public static final int RATE_LIMIT_SCG_FILTER_ORDER = METADATA_FIRST_FILTER_ORDER + 1;

    @Autowired(required = false)
    private LimitAPI limitAPI;

    @Override
    public int getOrder() {
        return RATE_LIMIT_SCG_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return limitAPI != null;
    }

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // get metadata of current thread
        MetadataContext metadataContext = exchange.getAttribute(MetadataConstant.HeaderName.METADATA_CONTEXT);

        String peerNamespace = metadataContext.getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_NAMESPACE);
        String peerService = metadataContext.getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_SERVICE);
        String peerPath = metadataContext.getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_PATH);
        Map<String, String> labels = null;
        if (StringUtils.isNotBlank(peerPath)) {
            labels = new HashMap<>();
            labels.put("method", peerPath);
        }

        try {
            QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI, peerNamespace, peerService, 1, labels,
                    null);
            if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                DataBuffer dataBuffer = response.bufferFactory().allocateBuffer()
                        .write((Consts.QUOTA_LIMITED_INFO + quotaResponse.getInfo()).getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(dataBuffer));
            }
        } catch (Throwable throwable) {
            //限流API调用出现异常，不应该影响业务流程的调用
            LOG.error("fail to rate limit with QuotaRequest[{}-{}-{}].", peerNamespace, peerService, peerPath,
                    throwable);
        }

        return chain.filter(exchange);
    }
}
