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

package com.tencent.cloud.plugin.faulttolerance.model;

public enum FaultToleranceStrategy {
	/**
	 * Fails directly. For downstream services without idempotence, fail fast is recommended.
	 */
	FAIL_FAST,

	/**
	 * If the request is wrong, it will be retried.
	 */
	FAIL_OVER,

	/**
	 * Sending multiple requests at the same time requires the user to configure the degree of parallelism.
	 * For example, if two requests are sent at the same time, whichever one returns first will return the result.
	 * If the first request is an exception, it will wait for another request, and if all are exceptions, an exception will be returned.
	 */
	FORKING
}
