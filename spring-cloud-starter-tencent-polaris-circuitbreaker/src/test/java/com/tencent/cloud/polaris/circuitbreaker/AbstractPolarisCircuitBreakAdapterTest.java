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

package com.tencent.cloud.polaris.circuitbreaker;

import com.tencent.cloud.polaris.circuitbreaker.config.PolarisCircuitBreakerProperties;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.http.HttpStatus;

/**
 * Test For {@link AbstractPolarisCircuitBreakAdapter}.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2022/7/11
 */
public class AbstractPolarisCircuitBreakAdapterTest {

	@Test
	public void testApplyWithDefaultConfig() {
		PolarisCircuitBreakerProperties properties = new PolarisCircuitBreakerProperties();
		// Mock Condition
		SimplePolarisCircuitBreakAdapter adapter = new SimplePolarisCircuitBreakAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutIgnoreInternalServerError() {
		PolarisCircuitBreakerProperties properties = new PolarisCircuitBreakerProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(false);

		SimplePolarisCircuitBreakAdapter adapter = new SimplePolarisCircuitBreakAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(true);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithIgnoreInternalServerError() {
		PolarisCircuitBreakerProperties properties = new PolarisCircuitBreakerProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(true);

		SimplePolarisCircuitBreakAdapter adapter = new SimplePolarisCircuitBreakAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutSeries() {
		PolarisCircuitBreakerProperties properties = new PolarisCircuitBreakerProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();

		SimplePolarisCircuitBreakAdapter adapter = new SimplePolarisCircuitBreakAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(false);
	}

	@Test
	public void testApplyWithSeries() {
		PolarisCircuitBreakerProperties properties = new PolarisCircuitBreakerProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();
		properties.getSeries().add(HttpStatus.Series.CLIENT_ERROR);

		SimplePolarisCircuitBreakAdapter adapter = new SimplePolarisCircuitBreakAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.FORBIDDEN)).isEqualTo(true);
	}

	/**
	 * Simple Polaris CircuitBreak Adapter Implements .
	 */
	public static class SimplePolarisCircuitBreakAdapter extends AbstractPolarisCircuitBreakAdapter {

		public SimplePolarisCircuitBreakAdapter(PolarisCircuitBreakerProperties properties) {
			super(properties);
		}
	}
}
