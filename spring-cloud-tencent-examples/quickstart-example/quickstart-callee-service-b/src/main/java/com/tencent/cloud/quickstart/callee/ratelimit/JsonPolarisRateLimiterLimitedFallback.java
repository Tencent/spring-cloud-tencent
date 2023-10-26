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

package com.tencent.cloud.quickstart.callee.ratelimit;

import com.tencent.cloud.polaris.ratelimit.spi.PolarisRateLimiterLimitedFallback;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * JsonPolarisRateLimiterLimitedFallback.
 *
 * @author Lingxiao.Wu
 */
@Component
public class JsonPolarisRateLimiterLimitedFallback implements PolarisRateLimiterLimitedFallback {
	@Override
	public MediaType mediaType() {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	public String rejectTips() {
		return "{\"success\":false,\"code\":429,\"msg\":\"RejectRequestTips提示消息\",\"timestamp\":1673415310479}";
	}
}
