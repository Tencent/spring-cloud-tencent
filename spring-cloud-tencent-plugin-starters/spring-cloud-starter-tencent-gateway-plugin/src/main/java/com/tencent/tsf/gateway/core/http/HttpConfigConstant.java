/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.tsf.gateway.core.http;

/**
 * @ClassName Config
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/8 11:48
 * @Version 1.0
 */
public final class HttpConfigConstant {
	/**
	 * HTTP_CONNECT_TIMEOUT.
	 */
	public static final int HTTP_CONNECT_TIMEOUT = 2000;
	/**
	 * HTTP_SOCKET_TIMEOUT.
	 */
	public static final int HTTP_SOCKET_TIMEOUT = 10000;
	/**
	 * HTTP_MAX_POOL_SIZE.
	 */
	public static final int HTTP_MAX_POOL_SIZE = 200;
	/**
	 * HTTP_MONITOR_INTERVAL.
	 */
	public static final int HTTP_MONITOR_INTERVAL = 5000;
	/**
	 * HTTP_IDLE_TIMEOUT.
	 */
	public static final int HTTP_IDLE_TIMEOUT = 30000;

	private HttpConfigConstant() {

	}
}
