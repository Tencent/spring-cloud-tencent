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

package com.tencent.cloud.rpc.enhancement.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;

/**
 * Properties of Polaris CircuitBreaker .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022-07-08
 */
@ConfigurationProperties("spring.cloud.tencent.rpc-enhancement.reporter")
public class RpcEnhancementReporterProperties {

	/**
	 * Whether report call result to polaris.
	 */
	private boolean enabled;

	/**
	 * Specify the Http status code(s) that needs to be reported as FAILED.
	 */
	private List<HttpStatus> statuses = new ArrayList<>();

	/**
	 * Specify List of HTTP status series that needs to be reported as FAILED when status list is empty.
	 */
	private List<HttpStatus.Series> series = toList(HttpStatus.Series.SERVER_ERROR);

	/**
	 * If ignore "Internal Server Error Http Status Code (500)",
	 * Only takes effect if the attribute {@link RpcEnhancementReporterProperties#series} is not empty.
	 */
	private boolean ignoreInternalServerError = true;

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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
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

	public boolean isIgnoreInternalServerError() {
		return ignoreInternalServerError;
	}

	public void setIgnoreInternalServerError(boolean ignoreInternalServerError) {
		this.ignoreInternalServerError = ignoreInternalServerError;
	}
}
