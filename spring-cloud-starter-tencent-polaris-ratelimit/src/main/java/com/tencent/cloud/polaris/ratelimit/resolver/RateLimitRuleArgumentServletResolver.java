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

package com.tencent.cloud.polaris.ratelimit.resolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.context.ServiceRuleManager;
import com.tencent.cloud.polaris.ratelimit.filter.QuotaCheckServletFilter;
import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLabelServletResolver;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;

/**
 * resolve arguments from rate limit rule for Servlet.
 *
 * @author seansyyu 2023-03-09
 */
public class RateLimitRuleArgumentServletResolver {

	private static final Logger LOG = LoggerFactory.getLogger(QuotaCheckServletFilter.class);

	private final ServiceRuleManager serviceRuleManager;

	private final PolarisRateLimiterLabelServletResolver labelResolver;

	public RateLimitRuleArgumentServletResolver(ServiceRuleManager serviceRuleManager, PolarisRateLimiterLabelServletResolver labelResolver) {
		this.serviceRuleManager = serviceRuleManager;
		this.labelResolver = labelResolver;
	}

	public Set<Argument> getArguments(HttpServletRequest request, String namespace, String service) {
		RateLimitProto.RateLimit rateLimitRule = serviceRuleManager.getServiceRateLimitRule(namespace, service);
		if (rateLimitRule == null) {
			return Collections.emptySet();
		}
		List<RateLimitProto.Rule> rules = rateLimitRule.getRulesList();
		if (CollectionUtils.isEmpty(rules)) {
			return Collections.emptySet();
		}
		return rules.stream()
				.flatMap(rule -> rule.getArgumentsList().stream())
				.map(matchArgument -> {
					String matchKey = matchArgument.getKey();
					Argument argument = null;
					switch (matchArgument.getType()) {
						case CUSTOM:
							argument = StringUtils.isBlank(matchKey) ? null :
									Argument.buildCustom(matchKey, Optional.ofNullable(getCustomResolvedLabels(request).get(matchKey)).orElse(StringUtils.EMPTY));
							break;
						case METHOD:
							argument = Argument.buildMethod(request.getMethod());
							break;
						case HEADER:
							argument = StringUtils.isBlank(matchKey) ? null :
									Argument.buildHeader(matchKey, Optional.ofNullable(request.getHeader(matchKey)).orElse(StringUtils.EMPTY));
							break;
						case QUERY:
							argument = StringUtils.isBlank(matchKey) ? null :
									Argument.buildQuery(matchKey, Optional.ofNullable(request.getParameter(matchKey)).orElse(StringUtils.EMPTY));
							break;
						case CALLER_SERVICE:
							String sourceServiceNamespace = MetadataContextHolder.getDisposableMetadata(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, true).orElse(StringUtils.EMPTY);
							String sourceServiceName = MetadataContextHolder.getDisposableMetadata(DEFAULT_METADATA_SOURCE_SERVICE_NAME, true).orElse(StringUtils.EMPTY);
							if (!StringUtils.isEmpty(sourceServiceNamespace) && !StringUtils.isEmpty(sourceServiceName)) {
								argument = Argument.buildCallerService(sourceServiceNamespace, sourceServiceName);
							}
							break;
						case CALLER_IP:
							argument = Argument.buildCallerIP(Optional.ofNullable(request.getRemoteAddr()).orElse(StringUtils.EMPTY));
							break;
						default:
							break;
					}
					return argument;
				}).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private Map<String, String> getCustomResolvedLabels(HttpServletRequest request) {
		if (labelResolver != null) {
			try {
				return labelResolver.resolve(request);
			}
			catch (Throwable e) {
				LOG.error("resolve custom label failed. resolver = {}", labelResolver.getClass().getName(), e);
			}
		}
		return Collections.emptyMap();
	}

}
