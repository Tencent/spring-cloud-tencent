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

package com.tencent.cloud.polaris.circuitbreaker.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BANDWIDTH_LIMIT_EXCEEDED;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.LOOP_DETECTED;
import static org.springframework.http.HttpStatus.NETWORK_AUTHENTICATION_REQUIRED;
import static org.springframework.http.HttpStatus.NOT_EXTENDED;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.VARIANT_ALSO_NEGOTIATES;

/**
 * Properties of Polaris CircuitBreaker .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022-07-08
 */
@ConfigurationProperties("spring.cloud.polaris.circuitbreaker")
public class PolarisCircuitBreakerProperties {

	/**
	 * If circuit-breaker enabled.
	 */
	private Boolean enabled = true;

	/**
	 * Specify the Http status code(s) that needs to be fused.
	 */
	private List<HttpStatus> statuses = toList(NOT_IMPLEMENTED, BAD_GATEWAY,
			SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, HTTP_VERSION_NOT_SUPPORTED, VARIANT_ALSO_NEGOTIATES,
			INSUFFICIENT_STORAGE, LOOP_DETECTED, BANDWIDTH_LIMIT_EXCEEDED, NOT_EXTENDED, NETWORK_AUTHENTICATION_REQUIRED);

	/**
	 * Specify List of HTTP status series.
	 */
	private List<HttpStatus.Series> series = toList(HttpStatus.Series.SERVER_ERROR);

	/**
	 * Ignore Internal Server Error Http Status Code,
	 * Only takes effect if the attribute {@link PolarisCircuitBreakerProperties#series} is not empty.
	 */
	private Boolean ignoreInternalServerError = true;

	/**
	 * Convert items to List.
	 *
	 * @param items item arrays
	 * @param <T>   Object Generics.
	 * @return list
	 */
	@SafeVarargs
	private static <T> List<T> toList(T... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public List<HttpStatus> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<HttpStatus> statuses) {
		this.statuses = statuses;
	}

	public List<HttpStatus.Series> getSeries() {
		return series;
	}

	public void setSeries(List<HttpStatus.Series> series) {
		this.series = series;
	}

	public Boolean getIgnoreInternalServerError() {
		return ignoreInternalServerError;
	}

	public void setIgnoreInternalServerError(Boolean ignoreInternalServerError) {
		this.ignoreInternalServerError = ignoreInternalServerError;
	}
}
