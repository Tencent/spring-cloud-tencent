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

package com.tencent.cloud.polaris.ratelimit.callee;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

import com.tencent.cloud.metadata.constant.MetadataConstant.SystemMetadataKey;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.polaris.ratelimit.utils.Consts;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Haotian Zhang
 */
@Order(QuotaCheckFilter.ORDER)
public class QuotaCheckFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaCheckFilter.class);

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private final LimitAPI limitAPI;

    public QuotaCheckFilter(LimitAPI limitAPI) {
        this.limitAPI = limitAPI;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        QuotaRequest quotaRequest = new QuotaRequest();
        String localNamespace = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.LOCAL_NAMESPACE);
        String localService = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.LOCAL_SERVICE);
        quotaRequest.setNamespace(localNamespace);
        quotaRequest.setService(localService);
        quotaRequest.setCount(1);

        String method = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.LOCAL_PATH);
        if (StringUtils.isNotBlank(method)) {
            Map<String, String> labels = new HashMap<>();
            labels.put("method", method);
            quotaRequest.setLabels(labels);
        }
        try {
            QuotaResponse quotaResponse = limitAPI.getQuota(quotaRequest);
            if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
                response.setStatus(TOO_MANY_REQUESTS.value());
                response.getWriter().write(Consts.QUOTA_LIMITED_INFO + quotaResponse.getInfo());
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (Throwable t) {
            //限流API调用出现异常，不应该影响业务流程的调用
            LOG.error("fail to invoke getQuota, service is " + localService, t);
            filterChain.doFilter(request, response);
        }
    }

}
