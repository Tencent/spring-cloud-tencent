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

package com.tencent.tsf.gateway.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author kysonli
 * 2019/4/9 12:29
 */
public class TsfGatewayException extends ResponseStatusException {
	private static final long serialVersionUID = 9210499285923741857L;

	private final TsfGatewayError gatewayError;

	public TsfGatewayException(TsfGatewayError gatewayError, Object... args) {
		this(gatewayError, null, args);
	}

	public TsfGatewayException(TsfGatewayError gatewayError, Throwable throwable, Object... args) {
		super(HttpStatus.resolve(gatewayError.getHttpStatus()), String.format("%s", String.format(gatewayError.getErrMsg(), args)), throwable);
		this.gatewayError = gatewayError;
	}

	public TsfGatewayError getGatewayError() {
		return gatewayError;
	}
}
