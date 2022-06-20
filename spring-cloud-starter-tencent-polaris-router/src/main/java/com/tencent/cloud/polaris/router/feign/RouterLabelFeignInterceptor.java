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

package com.tencent.cloud.polaris.router.feign;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.RouterConstants;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.RouterLabelResolver;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

/**
 * Resolver labels from request.
 *
 * @author lepdou 2022-05-12
 * @author cheese8 2022-06-20
 */
public class RouterLabelFeignInterceptor implements RequestInterceptor, Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(RouterLabelFeignInterceptor.class);

	private final List<RouterLabelResolver> routerLabelResolvers;
	private final MetadataLocalProperties metadataLocalProperties;
	private final RouterRuleLabelResolver routerRuleLabelResolver;

	public RouterLabelFeignInterceptor(List<RouterLabelResolver> routerLabelResolvers,
			MetadataLocalProperties metadataLocalProperties,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.sort(Comparator.comparingInt(Ordered::getOrder));
			this.routerLabelResolvers = routerLabelResolvers;
		}
		else {
			this.routerLabelResolvers = null;
		}
		this.metadataLocalProperties = metadataLocalProperties;
		this.routerRuleLabelResolver = routerRuleLabelResolver;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		// local service labels
		Map<String, String> labels = new HashMap<>(metadataLocalProperties.getContent());

		// labels from rule expression
		String peerServiceName = requestTemplate.feignTarget().name();
		Map<String, String> ruleExpressionLabels = getRuleExpressionLabels(requestTemplate, peerServiceName);
		labels.putAll(ruleExpressionLabels);

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(requestTemplate);
					if (!CollectionUtils.isEmpty(customResolvedLabels)) {
						labels.putAll(customResolvedLabels);
					}
				}
				catch (Throwable t) {
					LOGGER.error("[SCT][Router] revoke RouterLabelResolver occur some exception. ", t);
				}
			});
		}

		// labels from downstream
		Map<String, String> transitiveLabels = MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		labels.putAll(transitiveLabels);

		// pass label by header
		if (labels.size() == 0) {
			requestTemplate.header(RouterConstants.ROUTER_LABEL_HEADER);
			return;
		}
		
		String encodedLabelsContent;
		try {
			encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(labels), StandardCharsets.UTF_8.name());
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + StandardCharsets.UTF_8.name());
		}
		requestTemplate.header(RouterConstants.ROUTER_LABEL_HEADER, encodedLabelsContent);
	}

	private Map<String, String> getRuleExpressionLabels(RequestTemplate requestTemplate, String peerService) {
		Set<String> labelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerService);

		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return FeignExpressionLabelUtils.resolve(requestTemplate, labelKeys);
	}

}
