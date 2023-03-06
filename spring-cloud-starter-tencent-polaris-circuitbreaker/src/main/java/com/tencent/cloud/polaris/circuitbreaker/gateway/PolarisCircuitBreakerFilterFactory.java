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

package com.tencent.cloud.polaris.circuitbreaker.gateway;


import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import static java.util.Optional.ofNullable;
import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.containsEncodedParts;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.reset;

/**
 * PolarisCircuitBreakerFilterFactory.
 * mostly copy from SpringCloudCircuitBreakerFilterFactory, but create ReactiveCircuitBreaker per request to build method level CircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerFilterFactory extends SpringCloudCircuitBreakerFilterFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreakerFilterFactory.class);

	private String routeIdPrefix;

	private final ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory;

	private final ObjectProvider<DispatcherHandler> dispatcherHandlerProvider;

	// do not use this dispatcherHandler directly, use getDispatcherHandler() instead.
	private volatile DispatcherHandler dispatcherHandler;

	public PolarisCircuitBreakerFilterFactory(
			ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory,
			ObjectProvider<DispatcherHandler> dispatcherHandlerProvider,
			ReactiveDiscoveryClient discoveryClient,
			DiscoveryLocatorProperties properties
	) {
		super(reactiveCircuitBreakerFactory, dispatcherHandlerProvider);
		this.reactiveCircuitBreakerFactory = reactiveCircuitBreakerFactory;
		this.dispatcherHandlerProvider = dispatcherHandlerProvider;
		if (discoveryClient != null && properties != null) {
			if (StringUtils.hasText(properties.getRouteIdPrefix())) {
				routeIdPrefix = properties.getRouteIdPrefix();
			}
			else {
				routeIdPrefix = discoveryClient.getClass().getSimpleName() + "_";
			}
		}
	}

	private void addExceptionDetails(Throwable t, ServerWebExchange exchange) {
		ofNullable(t).ifPresent(
				exception -> exchange.getAttributes().put(CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR, exception));
	}

	private DispatcherHandler getDispatcherHandler() {
		if (dispatcherHandler == null) {
			dispatcherHandler = dispatcherHandlerProvider.getIfAvailable();
		}
		return dispatcherHandler;
	}

	private String getCircuitBreakerId(Config config) {
		if (!StringUtils.hasText(config.getName()) && StringUtils.hasText(config.getRouteId())) {
			if (routeIdPrefix != null && config.getRouteId().startsWith(routeIdPrefix)) {
				return config.getRouteId().replace(routeIdPrefix, "");
			}
			return config.getRouteId();
		}
		return config.getName();
	}

	private boolean isNumeric(String statusString) {
		try {
			Integer.parseInt(statusString);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	private List<HttpStatus> getSeriesStatus(String series) {
		if (!Arrays.asList("1**", "2**", "3**", "4**", "5**").contains(series)) {
			throw new InvalidPropertyException(Config.class, "statusCodes", "polaris circuit breaker status code can only be a numeric http status, or a http series pattern, e.g. [\"1**\",\"2**\",\"3**\",\"4**\",\"5**\"]");
		}
		HttpStatus[] allHttpStatus = HttpStatus.values();
		if (series.startsWith("1")) {
			return Arrays.stream(allHttpStatus).filter(HttpStatus::is1xxInformational).collect(Collectors.toList());
		}
		else if (series.startsWith("2")) {
			return Arrays.stream(allHttpStatus).filter(HttpStatus::is2xxSuccessful).collect(Collectors.toList());
		}
		else if (series.startsWith("3")) {
			return Arrays.stream(allHttpStatus).filter(HttpStatus::is3xxRedirection).collect(Collectors.toList());
		}
		else if (series.startsWith("4")) {
			return Arrays.stream(allHttpStatus).filter(HttpStatus::is4xxClientError).collect(Collectors.toList());
		}
		else if (series.startsWith("5")) {
			return Arrays.stream(allHttpStatus).filter(HttpStatus::is5xxServerError).collect(Collectors.toList());
		}
		return Arrays.asList(allHttpStatus);
	}

	@Override
	public GatewayFilter apply(Config config) {
		Set<HttpStatus> statuses = config.getStatusCodes().stream()
				.flatMap(statusCode -> {
					List<HttpStatus> httpStatuses = new ArrayList<>();
					if (isNumeric(statusCode)) {
						httpStatuses.add(HttpStatusHolder.parse(statusCode).getHttpStatus());
					}
					else {
						httpStatuses.addAll(getSeriesStatus(statusCode));
					}
					return httpStatuses.stream();
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		String circuitBreakerId = getCircuitBreakerId(config);
		return new GatewayFilter() {
			@Override
			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
				String path = exchange.getRequest().getPath().value();
				ReactiveCircuitBreaker cb = reactiveCircuitBreakerFactory.create(circuitBreakerId + "#" + path);
				return cb.run(chain.filter(exchange).doOnSuccess(v -> {
					if (statuses.contains(exchange.getResponse().getStatusCode())) {
						HttpStatus status = exchange.getResponse().getStatusCode();
						throw new CircuitBreakerStatusCodeException(status);
					}
				}), t -> {
					if (config.getFallbackUri() == null) {
						return Mono.error(t);
					}

					exchange.getResponse().setStatusCode(null);
					reset(exchange);

					// TODO: copied from RouteToRequestUrlFilter
					URI uri = exchange.getRequest().getURI();
					// TODO: assume always?
					boolean encoded = containsEncodedParts(uri);
					URI requestUrl = UriComponentsBuilder.fromUri(uri).host(null).port(null)
							.uri(config.getFallbackUri()).scheme(null).build(encoded).toUri();
					exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
					addExceptionDetails(t, exchange);

					// Reset the exchange
					reset(exchange);

					ServerHttpRequest request = exchange.getRequest().mutate().uri(requestUrl).build();
					return getDispatcherHandler().handle(exchange.mutate().request(request).build());
				}).onErrorResume(t -> handleErrorWithoutFallback(t, config.isResumeWithoutError()));
			}

			@Override
			public String toString() {
				return filterToStringCreator(PolarisCircuitBreakerFilterFactory.this)
						.append("name", config.getName()).append("fallback", config.getFallbackUri()).toString();
			}
		};

	}

	@Override
	protected Mono<Void> handleErrorWithoutFallback(Throwable t, boolean resumeWithoutError) {
		if (t instanceof java.util.concurrent.TimeoutException) {
			return Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, t.getMessage(), t));
		}
		if (t instanceof CallAbortedException) {
			LOGGER.debug("PolarisCircuitBreaker CallAbortedException: {}", t.getMessage());
			return Mono.error(new ServiceUnavailableException());
		}
		if (resumeWithoutError) {
			return Mono.empty();
		}
		return Mono.error(t);
	}

}
