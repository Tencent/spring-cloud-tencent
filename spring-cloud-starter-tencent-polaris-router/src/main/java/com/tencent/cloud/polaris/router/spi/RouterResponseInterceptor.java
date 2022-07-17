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

package com.tencent.cloud.polaris.router.spi;

import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;

/**
 * The interceptor for router response. Router plugin can modify router response by interceptor.
 *
 * @author lepdou 2022-07-11
 */
public interface RouterResponseInterceptor {

	/**
	 * processing router response.
	 *
	 * @param response the router response.
	 * @param routerContext the router context.
	 */
	void apply(ProcessRoutersResponse response, PolarisRouterContext routerContext);
}
