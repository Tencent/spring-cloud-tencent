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

package com.tencent.tsf.unit.core.exception;

public class TencentUnitException extends RuntimeException {

	private final ErrorCode code;

	public TencentUnitException(ErrorCode code) {
		this.code = code;
	}

	public TencentUnitException(ErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public TencentUnitException(ErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public String getMessage() {
		StringBuilder builder = new StringBuilder(String.format("ERR-%d(%s): ", code.getCode(), code.name()));
		builder.append(super.getMessage());
		Throwable cause = getCause();
		if (null != cause) {
			builder.append(", cause: ").append(cause.getMessage());
		}
		return builder.toString();
	}

	public ErrorCode getCode() {
		return code;
	}

}
