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

package com.tencent.cloud.polaris.ratelimit.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
import com.tencent.cloud.polaris.ratelimit.utils.QuotaCheckUtils;
import com.tencent.cloud.polaris.ratelimit.utils.RateLimitUtils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant.LABEL_METHOD;

/**
 * Servlet filter to check quota.
 *
 * @author Haotian Zhang
 */
@Order(RateLimitConstant.FILTER_ORDER)
public class QuotaCheckServletFilter extends OncePerRequestFilter {

	private static final Logger LOG = LoggerFactory
			.getLogger(QuotaCheckServletFilter.class);

	private final LimitAPI limitAPI;

	private final PolarisRateLimiterLabelServletResolver labelResolver;

	private final PolarisRateLimitProperties polarisRateLimitProperties;

	private String rejectTips;

	public QuotaCheckServletFilter(LimitAPI limitAPI,
			PolarisRateLimiterLabelServletResolver labelResolver,
			PolarisRateLimitProperties polarisRateLimitProperties) {
		this.limitAPI = limitAPI;
		this.labelResolver = labelResolver;
		this.polarisRateLimitProperties = polarisRateLimitProperties;
	}

	@PostConstruct
	public void init() {
		rejectTips = RateLimitUtils.getRejectTips(polarisRateLimitProperties);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String localNamespace = MetadataContext.LOCAL_NAMESPACE;
		String localService = MetadataContext.LOCAL_SERVICE;

		Map<String, String> labels = new HashMap<>();

		// add build in labels
		String path = request.getRequestURI();

		if (StringUtils.isNotBlank(path)) {
			labels.put(LABEL_METHOD, path);
		}

		// add custom labels
		if (labelResolver != null) {
			try {
				Map<String, String> customLabels = labelResolver.resolve(request);
				if (!CollectionUtils.isEmpty(customLabels)) {
					labels.putAll(customLabels);
				}
			}
			catch (Throwable e) {
				LOG.error("resolve custom label failed. resolver = {}",
						labelResolver.getClass().getName(), e);
			}
		}

		try {
			QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI,
					localNamespace, localService, 1, labels, null);
			if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
				response.setStatus(polarisRateLimitProperties.getRejectHttpCode());
				response.getWriter().write(rejectTips);
			}
			else {
				filterChain.doFilter(request, response);
			}
		}
		catch (Throwable t) {
			// An exception occurs in the rate limiting API call,
			// which should not affect the call of the business process.
			LOG.error("fail to invoke getQuota, service is " + localService, t);
			filterChain.doFilter(request, response);
		}
	}

}
