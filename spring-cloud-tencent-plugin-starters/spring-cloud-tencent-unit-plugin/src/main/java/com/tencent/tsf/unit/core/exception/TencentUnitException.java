/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
