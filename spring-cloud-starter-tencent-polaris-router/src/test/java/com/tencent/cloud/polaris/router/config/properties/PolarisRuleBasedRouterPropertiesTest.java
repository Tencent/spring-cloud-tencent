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

package com.tencent.cloud.polaris.router.config.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link PolarisRuleBasedRouterProperties}.
 */
public class PolarisRuleBasedRouterPropertiesTest {

	PolarisRuleBasedRouterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new PolarisRuleBasedRouterProperties();
	}

	@Test
	public void isEnabled() {
		assertThat(properties.isEnabled()).isEqualTo(true);
	}

	@Test
	public void setEnabled() {
		properties.setEnabled(false);
		assertThat(properties.isEnabled()).isEqualTo(false);
	}

	@Test
	public void testToString() {
		assertThat(properties.toString())
				.isEqualTo("PolarisNearByRouterProperties{enabled=true}");
	}

}
