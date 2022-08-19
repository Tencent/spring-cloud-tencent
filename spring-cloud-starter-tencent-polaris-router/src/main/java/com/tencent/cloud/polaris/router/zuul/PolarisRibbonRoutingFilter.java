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

package com.tencent.cloud.polaris.router.zuul;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.BeanFactoryUtils;
import com.tencent.cloud.common.util.expresstion.ServletExpressionLabelUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.ServletRouterLabelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RETRYABLE_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Replaces the default RibbonRoutingFilter implementation.
 *
 * @author jarvisxiong 2022-08-04
 */
public class PolarisRibbonRoutingFilter extends RibbonRoutingFilter implements BeanFactoryAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisRibbonRoutingFilter.class);
	private final StaticMetadataManager staticMetadataManager;
	private final RouterRuleLabelResolver routerRuleLabelResolver;
	private final List<ServletRouterLabelResolver> routerLabelResolvers;
	private BeanFactory factory;
	private boolean useServlet31 = true;

	public PolarisRibbonRoutingFilter(ProxyRequestHelper helper,
			RibbonCommandFactory<?> ribbonCommandFactory,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver,
			List<ServletRouterLabelResolver> routerLabelResolvers) {
		super(helper, ribbonCommandFactory, Collections.emptyList());
		this.staticMetadataManager = staticMetadataManager;
		this.routerRuleLabelResolver = routerRuleLabelResolver;

		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.sort(Comparator.comparingInt(Ordered::getOrder));
			this.routerLabelResolvers = routerLabelResolvers;
		}
		else {
			this.routerLabelResolvers = null;
		}
		// To support Servlet API 3.1 we need to check if getContentLengthLong exists
		// Spring 5 minimum support is 3.0, so this stays
		try {
			HttpServletRequest.class.getMethod("getContentLengthLong");
		}
		catch (NoSuchMethodException e) {
			useServlet31 = false;
		}
	}


	@Override
	protected RibbonCommandContext buildCommandContext(RequestContext context) {
		HttpServletRequest request = context.getRequest();

		MultiValueMap<String, String> headers = this.helper
				.buildZuulRequestHeaders(request);
		MultiValueMap<String, String> params = this.helper
				.buildZuulRequestQueryParams(request);
		String verb = getVerb(request);
		InputStream requestEntity = getRequestBody(request);
		if (request.getContentLength() < 0 && !verb.equalsIgnoreCase("GET")) {
			context.setChunkedRequestBody();
		}

		String serviceId = (String) context.get(SERVICE_ID_KEY);
		Boolean retryable = (Boolean) context.get(RETRYABLE_KEY);
		Object loadBalancerKey = context.get(LOAD_BALANCER_KEY);
		if (loadBalancerKey == null) {
			// By default, use the routerContext as loadBalancerKey
			loadBalancerKey = genRouterContext(request, serviceId);
		}

		String uri = this.helper.buildZuulRequestURI(request);

		// remove double slashes
		uri = uri.replace("//", "/");

		long contentLength = useServlet31 ? request.getContentLengthLong()
				: request.getContentLength();

		return new RibbonCommandContext(serviceId, verb, uri, retryable, headers, params,
				requestEntity, this.requestCustomizers, contentLength, loadBalancerKey);
	}

	PolarisRouterContext genRouterContext(HttpServletRequest request, String serviceId) {
		// local service labels
		Map<String, String> labels = new HashMap<>(staticMetadataManager.getMergedStaticMetadata());

		// labels from rule expression
		Set<String> expressionLabelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, serviceId);
		Map<String, String> ruleExpressionLabels = getExpressionLabels(request, expressionLabelKeys);
		if (!CollectionUtils.isEmpty(ruleExpressionLabels)) {
			labels.putAll(ruleExpressionLabels);
		}

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(request, expressionLabelKeys);
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

		PolarisRouterContext routerContext = new PolarisRouterContext();

		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labels);
		routerContext.putLabels(PolarisRouterContext.TRANSITIVE_LABELS, transitiveLabels);

		return routerContext;
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.factory = beanFactory;
	}

	private Map<String, String> getExpressionLabels(HttpServletRequest request, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		return ServletExpressionLabelUtils.resolve(request, labelKeys);
	}

	private void init() {
		this.requestCustomizers = BeanFactoryUtils.getBeans(factory, RibbonRequestCustomizer.class);
	}
}
