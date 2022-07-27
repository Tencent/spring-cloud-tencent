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

package com.tencent.cloud.rpc.enhancement;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.http.HttpStatus;

/**
 * Test For {@link AbstractPolarisReporterAdapter}.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022/7/11
 */
public class AbstractPolarisReporterAdapterTest {

	@Test
	public void testApplyWithDefaultConfig() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutIgnoreInternalServerError() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(false);

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(true);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithIgnoreInternalServerError() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(true);

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutSeries() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithSeries() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();
		properties.getSeries().add(HttpStatus.Series.CLIENT_ERROR);

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		// Assert
		Assertions.assertThat(adapter.apply(HttpStatus.OK)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(false);
		Assertions.assertThat(adapter.apply(HttpStatus.FORBIDDEN)).isEqualTo(true);
	}

	/**
	 * Simple Polaris CircuitBreak Adapter Implements .
	 */
	public static class SimplePolarisReporterAdapter extends AbstractPolarisReporterAdapter {

		public SimplePolarisReporterAdapter(RpcEnhancementReporterProperties properties) {
			super(properties);
		}
	}
}
