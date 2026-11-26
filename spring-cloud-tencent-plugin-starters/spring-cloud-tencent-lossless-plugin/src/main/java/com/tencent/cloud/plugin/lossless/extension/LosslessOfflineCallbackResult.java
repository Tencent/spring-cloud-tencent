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

package com.tencent.cloud.plugin.lossless.extension;

/**
 * Internal execution result of offline callbacks.
 *
 * @author evelynwei
 */
final class LosslessOfflineCallbackResult {

	enum Status {
		SUCCESS,
		CALLBACK_FAILED,
		TIMEOUT,
		BUDGET_EXHAUSTED,
		SUBMIT_REJECTED,
		TASK_ACTIVE,
		CLOSED,
		CALLER_INTERRUPTED
	}

	private final Status status;

	private final Throwable failure;

	private final boolean failAction;

	private final int successCount;

	private final int failureCount;

	private final int skippedCount;

	private final int notExecutedCount;

	private LosslessOfflineCallbackResult(Status status, Throwable failure, boolean failAction, int successCount,
			int failureCount, int skippedCount, int notExecutedCount) {
		this.status = status;
		this.failure = failure;
		this.failAction = failAction;
		this.successCount = successCount;
		this.failureCount = failureCount;
		this.skippedCount = skippedCount;
		this.notExecutedCount = notExecutedCount;
	}

	static LosslessOfflineCallbackResult success(int successCount) {
		return new LosslessOfflineCallbackResult(Status.SUCCESS, null, false, successCount, 0, 0, 0);
	}

	static LosslessOfflineCallbackResult of(Status status, Throwable failure, boolean failAction, int successCount,
			int failureCount, int skippedCount, int notExecutedCount) {
		return new LosslessOfflineCallbackResult(status, failure, failAction, successCount, failureCount,
				skippedCount, notExecutedCount);
	}

	Status getStatus() {
		return status;
	}

	Throwable getFailure() {
		return failure;
	}

	boolean isFailAction() {
		return failAction;
	}

	int getSuccessCount() {
		return successCount;
	}

	int getFailureCount() {
		return failureCount;
	}

	int getSkippedCount() {
		return skippedCount;
	}

	int getNotExecutedCount() {
		return notExecutedCount;
	}
}
