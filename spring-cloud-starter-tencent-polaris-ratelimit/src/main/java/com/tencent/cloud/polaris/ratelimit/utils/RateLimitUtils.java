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

package com.tencent.cloud.polaris.ratelimit.utils;

import com.tencent.cloud.common.util.ResourceFileUtils;
import com.tencent.cloud.polaris.ratelimit.config.PolarisRateLimitProperties;
import com.tencent.cloud.polaris.ratelimit.constant.RateLimitConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Rate limit utils.
 *
 * @author lepdou 2022-04-20
 */
public final class RateLimitUtils {

	private static final Logger LOG = LoggerFactory.getLogger(RateLimitUtils.class);

	private RateLimitUtils() {
	}

	public static String getRejectTips(PolarisRateLimitProperties polarisRateLimitProperties) {
		String tips = polarisRateLimitProperties.getRejectRequestTips();

		if (!StringUtils.isEmpty(tips)) {
			return tips;
		}

		String rejectFilePath = polarisRateLimitProperties.getRejectRequestTipsFilePath();
		if (!StringUtils.isEmpty(rejectFilePath)) {
			try {
				tips = ResourceFileUtils.readFile(rejectFilePath);
			}
			catch (Exception e) {
				LOG.error("[RateLimit] Read custom reject tips file error. path = {}",
						rejectFilePath, e);
			}
		}

		if (!StringUtils.isEmpty(tips)) {
			return tips;
		}

		return RateLimitConstant.QUOTA_LIMITED_INFO;
	}
}
