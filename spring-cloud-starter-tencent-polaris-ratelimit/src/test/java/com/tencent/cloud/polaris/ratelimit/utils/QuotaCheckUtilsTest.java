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

package com.tencent.cloud.polaris.ratelimit.utils;

import java.util.HashMap;
import java.util.HashSet;

import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link QuotaCheckUtils}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class QuotaCheckUtilsTest {

	private LimitAPI limitAPI;

	@BeforeEach
	void setUp() {
		limitAPI = mock(LimitAPI.class);
		when(limitAPI.getQuota(any(QuotaRequest.class))).thenAnswer(invocationOnMock -> {
			String serviceName = ((QuotaRequest) invocationOnMock.getArgument(0)).getService();
			if (serviceName.equals("TestApp1")) {
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "QuotaResultOk"));
			}
			else if (serviceName.equals("TestApp2")) {
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultOk, 1000, "QuotaResultOk"));
			}
			else if (serviceName.equals("TestApp3")) {
				return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, "QuotaResultLimited"));
			}
			else {
				throw new RuntimeException("Mock exception.");
			}
		});
	}

	@Test
	public void testGetQuota() {
		// Pass
		String serviceName = "TestApp1";
		QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashMap<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultOk");

		// Unirate waiting 1000ms
		serviceName = "TestApp2";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashMap<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(1000);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultOk");

		// Rate limited
		serviceName = "TestApp3";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashMap<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultLimited);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultLimited");

		// Exception
		serviceName = "TestApp4";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashMap<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("get quota failed");
	}

	@Test
	public void testGetQuota2() {
		// Pass
		String serviceName = "TestApp1";
		QuotaResponse quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashSet<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultOk");

		// Unirate waiting 1000ms
		serviceName = "TestApp2";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashSet<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(1000);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultOk");

		// Rate limited
		serviceName = "TestApp3";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashSet<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultLimited);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("QuotaResultLimited");

		// Exception
		serviceName = "TestApp4";
		quotaResponse = QuotaCheckUtils.getQuota(limitAPI, null, serviceName, 1, new HashSet<>(), null);
		assertThat(quotaResponse.getCode()).isEqualTo(QuotaResultCode.QuotaResultOk);
		assertThat(quotaResponse.getWaitMs()).isEqualTo(0);
		assertThat(quotaResponse.getInfo()).isEqualTo("get quota failed");
	}
}
