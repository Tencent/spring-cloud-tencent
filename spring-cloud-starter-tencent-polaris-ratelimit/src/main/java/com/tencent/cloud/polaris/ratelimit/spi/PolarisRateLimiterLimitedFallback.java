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

package com.tencent.cloud.polaris.ratelimit.spi;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * PolarisRateLimiterLimitedFallback.
 *
 * @author Lingxiao.Wu
 */
public interface PolarisRateLimiterLimitedFallback {

	/**
	 * Customized mediaType when polaris rateLimiter limited.
	 *
	 * @return Customized mediaType
	 */
	default MediaType mediaType() {
		return MediaType.TEXT_HTML;
	}

	/**
	 * Customized charset when polaris rateLimiter limited.
	 *
	 * @return Customized charset
	 */
	default Charset charset() {
		return StandardCharsets.UTF_8;
	}

	/**
	 * Customized rejectHttpCode when polaris rateLimiter limited.
	 *
	 * @return Customized rejectHttpCode
	 */
	default Integer rejectHttpCode() {
		return HttpStatus.TOO_MANY_REQUESTS.value();
	}

	/**
	 * Customized rejectTips when polaris rateLimiter limited.
	 *
	 * @return Customized rejectTips
	 */
	String rejectTips();
}
