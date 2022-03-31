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

package com.tencent.cloud.polaris.ratelimit.constant;

import org.springframework.core.Ordered;

/**
 * Constant for rate-limiter.
 *
 * @author Haotian Zhang
 */
public final class RateLimitConstant {

	/**
	 * Order of filter.
	 */
	public static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	/**
	 * Info of rate limit.
	 */
	public static String QUOTA_LIMITED_INFO = "The request is deny by rate limit because the throttling threshold is reached";

	/**
	 * The build in label method.
	 */
	public static String LABEL_METHOD = "method";

	/**
	 * The build in label path.
	 */
	public static String LABEL_PATH = "path";

	private RateLimitConstant() {
	}

}
