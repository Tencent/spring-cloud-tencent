/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.circuitbreaker.common;

import com.tencent.polaris.circuitbreak.api.pojo.ResultToErrorCode;
import feign.FeignException;

import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * PolarisResultToErrorCode.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisResultToErrorCode implements ResultToErrorCode {

	@Override
	public int onSuccess(Object value) {
		return 200;
	}

	@Override
	public int onError(Throwable e) {
		if (checkClassExist("org.springframework.web.client.RestClientResponseException")
				&& e instanceof RestClientResponseException) {
			return ((RestClientResponseException) e).getRawStatusCode();
		}
		else if (checkClassExist("feign.FeignException")
				&& e instanceof FeignException) {
			return ((FeignException) e).status();
		}
		else if (checkClassExist("org.springframework.web.reactive.function.client.WebClientResponseException")
				&& e instanceof WebClientResponseException) {
			return ((WebClientResponseException) e).getRawStatusCode();
		}
		return -1;
	}

	private boolean checkClassExist(String clazzName) {
		try {
			Class.forName(clazzName, false, getClass().getClassLoader());
		}
		catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}

}
