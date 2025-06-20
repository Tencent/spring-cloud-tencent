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

package com.tencent.tsf.gateway.core.exception;

/**
 * @author kysonli
 * 2019/4/9 13:30
 */
public enum TsfGatewayError {
	/**
	 * GATEWAY_INTERNAL_ERROR.
	 */
	GATEWAY_INIT_ERROR(5000, "InitializeError: %s", 500),
	/**
	 * GATEWAY_SYNC_ERROR.
	 */
	GATEWAY_SYNC_ERROR(5001, "SyncDataError: %s", 500),
	/**
	 * GATEWAY_FILE_ERROR.
	 */
	GATEWAY_FILE_ERROR(5002, "LocalFile IO Error", 500),
	/**
	 * GATEWAY_SERIALIZE_ERROR.
	 */
	GATEWAY_SERIALIZE_ERROR(5003, "SerializeError: %s", 500),
	/**
	 * GATEWAY_AUTH_ERROR.
	 */
	GATEWAY_AUTH_ERROR(5004, "AuthCheckException: %s", 500),
	/**
	 * GATEWAY_INTERNAL_ERROR.
	 */
	GATEWAY_INTERNAL_ERROR(5005, "Gateway Internal Error: %s", 500),
	/**
	 * GATEWAY_PARAMETER_REQUIRED.
	 */
	GATEWAY_PARAMETER_REQUIRED(5006, "ParameterRequired: %s", 500),
	/**
	 * GATEWAY_PARAMETER_INVALID.
	 */
	GATEWAY_PARAMETER_INVALID(5007, "ParameterInvalid: %s", 500),
	/**
	 * GATEWAY_BAD_REQUEST.
	 */
	GATEWAY_BAD_REQUEST(4000, "BadRequest: %s", 400),
	/**
	 * GATEWAY_AUTH_FAILED.
	 */
	GATEWAY_AUTH_FAILED(4001, "AuthCheckFailed: %s", 401),
	/**
	 * GATEWAY_REQUEST_FORBIDDEN.
	 */
	GATEWAY_REQUEST_FORBIDDEN(4003, "RequestForbidden: %s", 403),
	/**
	 * GATEWAY_REQUEST_NOT_FOUND.
	 */
	GATEWAY_REQUEST_NOT_FOUND(4004, "RequestNotFound: %s", 404),
	/**
	 * GATEWAY_REQUEST_METHOD_NOT_ALLOWED.
	 */
	GATEWAY_REMOTE_REQUEST_INVALID(4016, "RemoteRequestInvalid: %s", 416),
	/**
	 * GATEWAY_REMOTE_SERVER_AUTH_FAILED.
	 */
	GATEWAY_REMOTE_SERVER_AUTH_FAILED(4017, "RemoteServerAuthFailed: %s", 401),
	/**
	 * GATEWAY_PLUGIN_JWT.
	 */
	GATEWAY_PLUGIN_JWT(4020, "JwtError: %s", 500),
	/**
	 * GATEWAY_REMOTE_SERVER_BUSY.
	 */
	GATEWAY_REMOTE_SERVER_BUSY(4021, "Server is busy: %s", 401),
	/**
	 * GATEWAY_REMOTE_SERVER_CODE_INVALID.
	 */
	GATEWAY_REMOTE_SERVER_CODE_INVALID(4022, "Login Code is error: %s", 401),
	/**
	 * GATEWAY_REMOTE_SERVER_LIMIT.
	 */
	GATEWAY_REMOTE_SERVER_LIMIT(4023, "Server Limit : %s", 401),
	/**
	 * GATEWAY_AUTH_EXPIRED.
	 */
	GATEWAY_AUTH_EXPIRED(4024, "AuthCheckExpired : %s", 401),
	/**
	 * GATEWAY_TOO_MANY_REQUEST.
	 */
	GATEWAY_TOO_MANY_REQUEST(4025, "AuthCheckExpired : %s", 429);


	private Integer code;

	private String errMsg;

	private Integer httpStatus;

	TsfGatewayError(Integer code, String errMsg, Integer httpStatus) {
		this.code = code;
		this.errMsg = errMsg;
		this.httpStatus = httpStatus;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(Integer httpStatus) {
		this.httpStatus = httpStatus;
	}
}
