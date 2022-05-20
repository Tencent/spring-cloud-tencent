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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ExpressionLabelUtils;
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
 *@author lepdou 2022-05-12
 */
public class RouterLabelInterceptor implements RequestInterceptor, Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(RouterLabelInterceptor.class);

	private final RouterLabelResolver resolver;
	private final MetadataLocalProperties metadataLocalProperties;
	private final RouterRuleLabelResolver routerRuleLabelResolver;

	public RouterLabelInterceptor(RouterLabelResolver resolver,
			MetadataLocalProperties metadataLocalProperties,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		this.resolver = resolver;
		this.metadataLocalProperties = metadataLocalProperties;
		this.routerRuleLabelResolver = routerRuleLabelResolver;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		Map<String, String> labels = new HashMap<>();

		// labels from downstream
		Map<String, String> transitiveLabels = MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		labels.putAll(transitiveLabels);

		// labels from request
		if (resolver != null) {
			try {
				Map<String, String> customResolvedLabels = resolver.resolve(requestTemplate);
				if (!CollectionUtils.isEmpty(customResolvedLabels)) {
					labels.putAll(customResolvedLabels);
				}
			}
			catch (Throwable t) {
				LOGGER.error("[SCT][Router] revoke RouterLabelResolver occur some exception. ", t);
			}
		}

		// labels from rule expression
		String peerServiceName = requestTemplate.feignTarget().name();
		Map<String, String> ruleExpressionLabels = getRuleExpressionLabels(requestTemplate, peerServiceName);
		labels.putAll(ruleExpressionLabels);


		//local service labels
		labels.putAll(metadataLocalProperties.getContent());

		// Because when the label is placed in RequestTemplate.header,
		// RequestTemplate will parse the header according to the regular, which conflicts with the expression.
		// Avoid conflicts by escaping.
		Map<String, String> escapeLabels = new HashMap<>(labels.size());
		for (Map.Entry<String, String> entry : labels.entrySet()) {
			String escapedKey = ExpressionLabelUtils.escape(entry.getKey());
			String escapedValue = ExpressionLabelUtils.escape(entry.getValue());
			escapeLabels.put(escapedKey, escapedValue);
		}

		// pass label by header
		requestTemplate.header(RouterConstants.ROUTER_LABEL_HEADER, JacksonUtils.serialize2Json(escapeLabels));
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
