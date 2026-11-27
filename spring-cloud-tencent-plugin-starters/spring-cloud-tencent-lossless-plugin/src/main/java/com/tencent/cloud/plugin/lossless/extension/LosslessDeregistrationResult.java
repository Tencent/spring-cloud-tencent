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

import java.util.Objects;

/**
 * Result of a strict lossless deregistration.
 *
 * @author evelynwei
 */
public final class LosslessDeregistrationResult {

	private static final LosslessDeregistrationResult NOT_EXECUTED =
			new LosslessDeregistrationResult(false, null);

	private static final LosslessDeregistrationResult SUCCESS =
			new LosslessDeregistrationResult(true, null);

	private final boolean executed;

	private final Exception failure;

	private LosslessDeregistrationResult(boolean executed, Exception failure) {
		this.executed = executed;
		this.failure = failure;
	}

	public static LosslessDeregistrationResult success() {
		return SUCCESS;
	}

	public static LosslessDeregistrationResult notExecuted() {
		return NOT_EXECUTED;
	}

	public static LosslessDeregistrationResult failed(Exception failure) {
		return new LosslessDeregistrationResult(true, Objects.requireNonNull(failure));
	}

	public boolean isExecuted() {
		return executed;
	}

	public Exception getFailure() {
		return failure;
	}
}
