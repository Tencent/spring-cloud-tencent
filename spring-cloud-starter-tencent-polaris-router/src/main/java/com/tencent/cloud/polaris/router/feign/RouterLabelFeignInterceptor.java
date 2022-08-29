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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstants;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.FeignRouterLabelResolver;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Resolver labels from request.
 *
 * @author lepdou, cheese8
 */
public class RouterLabelFeignInterceptor implements RequestInterceptor, Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(RouterLabelFeignInterceptor.class);

	private final List<FeignRouterLabelResolver> routerLabelResolvers;
	private final StaticMetadataManager staticMetadataManager;
	private final RouterRuleLabelResolver routerRuleLabelResolver;

	public RouterLabelFeignInterceptor(List<FeignRouterLabelResolver> routerLabelResolvers,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver) {
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.sort(Comparator.comparingInt(Ordered::getOrder));
			this.routerLabelResolvers = routerLabelResolvers;
		}
		else {
			this.routerLabelResolvers = null;
		}
		this.staticMetadataManager = staticMetadataManager;
		this.routerRuleLabelResolver = routerRuleLabelResolver;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void apply(RequestTemplate requestTemplate) {
		// local service labels
		Map<String, String> labels = new HashMap<>(staticMetadataManager.getMergedStaticMetadata());

		// labels from rule expression
		String peerServiceName = requestTemplate.feignTarget().name();
		Set<String> expressionLabelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerServiceName);
		Map<String, String> ruleExpressionLabels = getRuleExpressionLabels(requestTemplate, expressionLabelKeys);
		labels.putAll(ruleExpressionLabels);

		// labels from custom spi
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(requestTemplate, expressionLabelKeys);
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
		String encodedLabelsContent;
		try {
			encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(labels), UTF_8);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + UTF_8);
		}
		requestTemplate.header(RouterConstants.ROUTER_LABEL_HEADER, encodedLabelsContent);
	}

	private Map<String, String> getRuleExpressionLabels(RequestTemplate requestTemplate, Set<String> labelKeys) {

		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return FeignExpressionLabelUtils.resolve(requestTemplate, labelKeys);
	}
}
