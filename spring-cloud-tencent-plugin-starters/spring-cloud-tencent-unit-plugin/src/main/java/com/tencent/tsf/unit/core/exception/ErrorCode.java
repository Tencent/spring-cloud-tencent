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

public enum ErrorCode {
	/**
	 * 内部加载配置错误.
	 */
	LOAD_ERROR(10000),
	/**
	 * 客户直接或间接输入的通用参数错误.
	 */
	COMMON_PARAMETER_ERROR(10001),
	/**
	 * 客户号映射服务为空.
	 */
	MAPPING_SERVICE_EMPTY_ERROR(10002),
	/**
	 * 客户号映射服务的 url 为空.
	 */
	MAPPING_URL_EMPTY_ERROR(10003),
	/**
	 * 客户号映射服务的响应码错误.
	 */
	MAPPING_RESPONSE_CODE_ERROR(10004),
	/**
	 * 客户号映射服务的响应体空错误.
	 */
	MAPPING_RESPONSE_EMPTY_BODY_ERROR(10005),
	/**
	 * 客户号服务响应体解析后无 customer number.
	 */
	MAPPING_RESPONSE_CUSTOMER_NUMBER_EMPTY_ERROR(10006),
	/**
	 * 客户号服务请求出现 IO 错误.
	 */
	MAPPING_REQUEST_IO_ERROR(10007),
	/**
	 * 客户号转换错误.
	 */
	CUSTOMER_NUMBER_TRANSFORM_ERROR(10008),
	/**
	 * 客户要素解析错误.
	 */
	CUSTOMER_IDENTIFIER_TRANSFORM_ERROR(10009);

	private final int code;

	ErrorCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
