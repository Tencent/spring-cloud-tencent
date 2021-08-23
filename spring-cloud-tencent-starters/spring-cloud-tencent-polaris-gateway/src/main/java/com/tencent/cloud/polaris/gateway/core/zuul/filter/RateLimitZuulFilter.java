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
package com.tencent.cloud.polaris.gateway.core.zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.polaris.ratelimit.utils.Consts;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RIBBON_ROUTING_FILTER_ORDER;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Zuul filter used for rate limit.
 *
 * @author Haotian Zhang
 */
public class RateLimitZuulFilter extends ZuulFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitZuulFilter.class);

    @Autowired(required = false)
    private LimitAPI limitAPI;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return RIBBON_ROUTING_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return limitAPI != null;
    }

    @Override
    public Object run() {
        // get request context
        RequestContext requestContext = RequestContext.getCurrentContext();

        String peerNamespace =
                MetadataContextHolder.get().getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_NAMESPACE);
        String peerService =
                MetadataContextHolder.get().getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_SERVICE);
        String peerPath = MetadataContextHolder.get().getSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_PATH);
        Map<String, String> labels = null;
        if (StringUtils.isNotBlank(peerPath)) {
            labels = new HashMap<>();
            labels.put("method", peerPath);
        }

        try {
            QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI, peerNamespace, peerService, 1, labels,
                    null);
            if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
                requestContext.setSendZuulResponse(false);
                requestContext.setResponseStatusCode(TOO_MANY_REQUESTS.value());
                requestContext.getResponse().getWriter().write(Consts.QUOTA_LIMITED_INFO + quotaResponse.getInfo());
            }
        } catch (Throwable throwable) {
            //限流API调用出现异常，不应该影响业务流程的调用
            LOG.error("fail to rate limit with QuotaRequest[{}-{}-{}].", peerNamespace, peerService, peerPath,
                    throwable);
        }

        return null;
    }
}
