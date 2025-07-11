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

package com.tencent.tsf.gateway.core.constant;

public final class HeaderName {
	/**
	 * UNIT.
	 */
	public static final String UNIT = "TSF-Unit";
	/**
	 * NAMESPACE_ID.
	 */
	public static final String NAMESPACE_ID = "TSF-NamespaceId";
	/**
	 * APP_KEY.
	 */
	public static final String APP_KEY = "x-mg-secretid";
	/**
	 * ALG.
	 */
	public static final String ALG = "x-mg-alg";
	/**
	 * SIGN.
	 */
	public static final String SIGN = "x-mg-sign";
	/**
	 * NONCE.
	 */
	public static final String NONCE = "x-mg-nonce";
	/**
	 * NODE.
	 */
	public static final String NODE = "x-mg-node";
	/**
	 * TRACE_ID.
	 */
	public static final String TRACE_ID = "x-mg-traceid";


	private HeaderName() {
	}
}
