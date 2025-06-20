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

package com.tencent.tsf.gateway.core.constant;

/**
 * @author kysonli
 * 2019/4/11 11:42
 */
public enum TsfAlgType {
	/**
	 * 0:HMAC_MD5.
	 */
	HMAC_MD5("0"),
	/**
	 * 1:HMAC_SHA_1.
	 */
	HMAC_SHA_1("1"),
	/**
	 * 2:HMAC_SHA_256.
	 */
	HMAC_SHA_256("2"),
	/**
	 * 3:HMAC_SHA_512.
	 */
	HMAC_SHA_512("3"),
	/**
	 * 4:HMAC_SM3.
	 */
	HMAC_SM3("4");


	private String alg;

	TsfAlgType(String code) {
		this.alg = code;
	}

	public static TsfAlgType getSecurityCode(String code) {
		for (TsfAlgType algType : TsfAlgType.values()) {
			if (algType.alg.equals(code)) {
				return algType;
			}
		}
		return null;
	}

	public String getAlg() {
		return alg;
	}

	public void setAlg(String alg) {
		this.alg = alg;
	}
}
