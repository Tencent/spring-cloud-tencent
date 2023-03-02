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

import java.io.IOException;

import com.tencent.cloud.common.util.ResourceFileUtils;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant.QUOTA_LIMITED_INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RateLimitUtils}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class RateLimitUtilsTest {

	private static MockedStatic<ResourceFileUtils> mockedResourceFileUtils;

	@BeforeAll
	static void beforeAll() throws IOException {
		mockedResourceFileUtils = mockStatic(ResourceFileUtils.class);
		when(ResourceFileUtils.readFile(anyString())).thenAnswer(invocation -> {
			String rejectFilePath = invocation.getArgument(0).toString();
			if (rejectFilePath.equals("exception.html")) {
				throw new IOException("Mock exceptions");
			}
			else {
				return "RejectRequestTips";
			}
		});
	}

	@AfterAll
	static void afterAll() {
		mockedResourceFileUtils.close();
	}

	@Test
	public void testGetRejectTips() {
		PolarisRateLimitProperties polarisRateLimitProperties = new PolarisRateLimitProperties();

		// RejectRequestTips
		polarisRateLimitProperties.setRejectRequestTips("RejectRequestTips");
		assertThat(RateLimitUtils.getRejectTips(polarisRateLimitProperties)).isEqualTo("RejectRequestTips");

		// RejectRequestTipsFilePath
		polarisRateLimitProperties.setRejectRequestTips(null);
		polarisRateLimitProperties.setRejectRequestTipsFilePath("reject-tips.html");
		assertThat(RateLimitUtils.getRejectTips(polarisRateLimitProperties)).isEqualTo("RejectRequestTips");

		// RejectRequestTipsFilePath with Exception
		polarisRateLimitProperties.setRejectRequestTips(null);
		polarisRateLimitProperties.setRejectRequestTipsFilePath("exception.html");
		assertThat(RateLimitUtils.getRejectTips(polarisRateLimitProperties)).isEqualTo(QUOTA_LIMITED_INFO);
	}
}
