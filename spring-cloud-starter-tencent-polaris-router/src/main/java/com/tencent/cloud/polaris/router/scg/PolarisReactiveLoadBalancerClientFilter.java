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

package com.tencent.cloud.polaris.router.scg;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.expresstion.ExpressionLabelUtils;
import com.tencent.cloud.common.util.expresstion.SpringWebExpressionLabelUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.PolarisRouterServiceInstanceListSupplier;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.SpringWebRouterLabelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 * ReactiveLoadBalancerClientFilter does not have the ability to pass route labels, so it is replaced
 * with PolarisReactiveLoadBalancerClientFilter. The passed route labels are used in
 * {@link PolarisRouterServiceInstanceListSupplier}.
 *
 * @author lepdou, Hoatian Zhang
 */
public class PolarisReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {
	private static final Logger log = LoggerFactory.getLogger(PolarisReactiveLoadBalancerClientFilter.class);

	private final LoadBalancerClientFactory clientFactory;
	private final GatewayLoadBalancerProperties gatewayLoadBalancerProperties;
	private final StaticMetadataManager staticMetadataManager;
	private final RouterRuleLabelResolver routerRuleLabelResolver;
	private final List<SpringWebRouterLabelResolver> routerLabelResolvers;
	private final PolarisContextProperties polarisContextProperties;

	public PolarisReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory,
			GatewayLoadBalancerProperties gatewayLoadBalancerProperties,
			StaticMetadataManager staticMetadataManager,
			RouterRuleLabelResolver routerRuleLabelResolver,
			List<SpringWebRouterLabelResolver> routerLabelResolvers,
			PolarisContextProperties polarisContextProperties) {
		super(clientFactory, gatewayLoadBalancerProperties);

		this.clientFactory = clientFactory;
		this.gatewayLoadBalancerProperties = gatewayLoadBalancerProperties;
		this.staticMetadataManager = staticMetadataManager;
		this.routerRuleLabelResolver = routerRuleLabelResolver;
		this.routerLabelResolvers = routerLabelResolvers;
		this.polarisContextProperties = polarisContextProperties;
	}

	/**
	 * Copied from ReactiveLoadBalancerClientFilter, and create new RequestData for passing router labels.
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
		String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
		if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
			return chain.filter(exchange);
		}
		// preserve the original url
		addOriginalRequestUrl(exchange, url);

		if (log.isTraceEnabled()) {
			log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
		}

		URI requestUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
		String serviceId = requestUri.getHost();
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
				.getSupportedLifecycleProcessors(clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
						RequestDataContext.class, ResponseData.class, ServiceInstance.class);

		// Pass route tags through http headers
		HttpHeaders routerHttpHeaders = genRouterHttpHeaders(exchange, serviceId);

		ServerHttpRequest request = exchange.getRequest();
		RequestData requestData = new RequestData(request.getMethod(), request.getURI(), routerHttpHeaders,
				new HttpHeaders(), new HashMap<>());
		DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(new RequestDataContext(
				requestData, getHint(serviceId)));

		return choose(lbRequest, serviceId, supportedLifecycleProcessors).doOnNext(response -> {

					if (!response.hasServer()) {
						supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
								.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, response)));
						throw NotFoundException.create(gatewayLoadBalancerProperties.isUse404(),
								"Unable to find instance for " + url.getHost());
					}

					ServiceInstance retrievedInstance = response.getServer();

					URI uri = exchange.getRequest().getURI();

					// if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
					// if the loadbalancer doesn't provide one.
					String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
					if (schemePrefix != null) {
						overrideScheme = url.getScheme();
					}

					DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(retrievedInstance,
							overrideScheme);

					URI requestUrl = reconstructURI(serviceInstance, uri);

					if (log.isTraceEnabled()) {
						log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
					}
					exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
					exchange.getAttributes().put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
					supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, response));
				}).then(chain.filter(exchange))
				.doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
						.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
								CompletionContext.Status.FAILED, throwable, lbRequest,
								exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR)))))
				.doOnSuccess(aVoid -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
						.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
								CompletionContext.Status.SUCCESS, lbRequest,
								exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR),
								new ResponseData(exchange.getResponse(), new RequestData(exchange.getRequest()))))));
	}

	@Override
	protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId,
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
		ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceId,
				ReactorServiceInstanceLoadBalancer.class);
		if (loadBalancer == null) {
			throw new NotFoundException("No loadbalancer available for " + serviceId);
		}
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		return loadBalancer.choose(lbRequest);
	}

	// no actual used
	private String getHint(String serviceId) {
		LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
		Map<String, String> hints = loadBalancerProperties.getHint();
		String defaultHint = hints.getOrDefault("default", "default");
		String hintPropertyValue = hints.get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

	// In order to be consistent with feign and restTemplate,
	// the router label is passed through the http header uniformly instead of the original hint mechanism.
	HttpHeaders genRouterHttpHeaders(ServerWebExchange exchange, String peerServiceName) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(RouterConstant.ROUTER_LABEL_HEADER, genRouterHint(exchange, peerServiceName));
		return headers;
	}

	private String genRouterHint(ServerWebExchange exchange, String peerServiceName) {
		Map<String, String> routerLabels = genRouterLabels(exchange, peerServiceName);
		String encodedLabelsContent;
		try {
			encodedLabelsContent = URLEncoder.encode(JacksonUtils.serialize2Json(routerLabels), UTF_8);
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + UTF_8);
		}
		return encodedLabelsContent;
	}

	private Map<String, String> genRouterLabels(ServerWebExchange exchange, String peerServiceName) {
		// local service labels
		Map<String, String> labels = new HashMap<>(staticMetadataManager.getMergedStaticMetadata());

		// labels from rule expression
		Set<String> expressionLabelKeys = routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, peerServiceName);

		Map<String, String> ruleExpressionLabels = getExpressionLabels(exchange, expressionLabelKeys);
		if (!CollectionUtils.isEmpty(ruleExpressionLabels)) {
			labels.putAll(ruleExpressionLabels);
		}

		// labels from request
		if (!CollectionUtils.isEmpty(routerLabelResolvers)) {
			routerLabelResolvers.forEach(resolver -> {
				try {
					Map<String, String> customResolvedLabels = resolver.resolve(exchange, expressionLabelKeys);
					if (!CollectionUtils.isEmpty(customResolvedLabels)) {
						labels.putAll(customResolvedLabels);
					}
				}
				catch (Throwable t) {
					log.error("[SCT][Router] revoke RouterLabelResolver occur some exception. ", t);
				}
			});
		}

		// labels from downstream
		Map<String, String> transitiveLabels = MetadataContextHolder.get().getTransitiveMetadata();
		labels.putAll(transitiveLabels);

		return labels;
	}

	private Map<String, String> getExpressionLabels(ServerWebExchange exchange, Set<String> labelKeys) {
		if (CollectionUtils.isEmpty(labelKeys)) {
			return Collections.emptyMap();
		}

		//enrich labels from request
		Map<String, String> labels = SpringWebExpressionLabelUtils.resolve(exchange, labelKeys);

		//enrich caller ip label
		for (String labelKey : labelKeys) {
			if (ExpressionLabelUtils.isCallerIPLabel(labelKey)) {
				labels.put(labelKey, polarisContextProperties.getLocalIpAddress());
			}
		}

		return labels;
	}
}
