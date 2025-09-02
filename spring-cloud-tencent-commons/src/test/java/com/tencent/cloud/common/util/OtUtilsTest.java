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

package com.tencent.cloud.common.util;

import org.junit.jupiter.api.Test;

import static com.tencent.cloud.common.util.OtUtils.OTEL_RESOURCE_ATTRIBUTES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link OtUtils}.
 *
 * @author Haotian Zhang
 */
public class OtUtilsTest {

	@Test
	public void testOtUtils() {
		String serviceName = "test-service";
		System.setProperty(OTEL_RESOURCE_ATTRIBUTES, "service.name=" + serviceName);

		OtUtils.setOtServiceNameIfNeeded(serviceName);

		assertThat(OtUtils.getOtServiceName()).isEqualTo(serviceName);
	}
}
