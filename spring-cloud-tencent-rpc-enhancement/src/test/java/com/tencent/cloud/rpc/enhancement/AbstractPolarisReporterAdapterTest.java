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

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.pojo.RetStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
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

	@Test
	public void testGetRetStatusFromRequest() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();
		properties.getSeries().add(HttpStatus.Series.CLIENT_ERROR);

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		HttpHeaders headers = new HttpHeaders();
		RetStatus ret = adapter.getRetStatusFromRequest(headers, RetStatus.RetFail);
		Assertions.assertThat(ret).isEqualTo(RetStatus.RetFail);

		headers.set(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetFlowControl.getDesc());
		ret = adapter.getRetStatusFromRequest(headers, RetStatus.RetFail);
		Assertions.assertThat(ret).isEqualTo(RetStatus.RetFlowControl);

		headers.set(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetReject.getDesc());
		ret = adapter.getRetStatusFromRequest(headers, RetStatus.RetFail);
		Assertions.assertThat(ret).isEqualTo(RetStatus.RetReject);
	}

	@Test
	public void testGetActiveRuleNameFromRequest() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();
		properties.getSeries().add(HttpStatus.Series.CLIENT_ERROR);

		SimplePolarisReporterAdapter adapter = new SimplePolarisReporterAdapter(properties);

		HttpHeaders headers = new HttpHeaders();
		String ruleName = adapter.getActiveRuleNameFromRequest(headers);
		Assertions.assertThat(ruleName).isEqualTo("");

		headers.set(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME, "mock_rule");
		ruleName = adapter.getActiveRuleNameFromRequest(headers);
		Assertions.assertThat(ruleName).isEqualTo("mock_rule");
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
