/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.common.tsf;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link TsfContextUtils}.
 */
public class TsfContextUtilsTest {

	@BeforeEach
	@AfterEach
	public void reset() throws Exception {
		Field isTsfConsulEnabledFirst = TsfContextUtils.class.getDeclaredField("isTsfConsulEnabledFirstConfiguration");
		isTsfConsulEnabledFirst.setAccessible(true);
		((AtomicBoolean) isTsfConsulEnabledFirst.get(null)).set(true);

		Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
		tsfConsulEnabledField.setAccessible(true);
		tsfConsulEnabledField.set(null, false);
	}

	@Test
	public void testIsTsfHeaderCompatibleWhenConfigured() {
		assertThat(TsfContextUtils.isTsfHeaderCompatible(true)).isTrue();
		assertThat(TsfContextUtils.isTsfConsulEnabled()).isFalse();
	}

	@Test
	public void testIsTsfHeaderCompatibleWhenConsulEnabled() throws Exception {
		Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
		tsfConsulEnabledField.setAccessible(true);
		tsfConsulEnabledField.set(null, true);

		assertThat(TsfContextUtils.isTsfHeaderCompatible(false)).isTrue();
	}

	@Test
	public void testIsTsfHeaderCompatibleWhenDisabled() {
		assertThat(TsfContextUtils.isTsfHeaderCompatible(false)).isFalse();
	}
}
