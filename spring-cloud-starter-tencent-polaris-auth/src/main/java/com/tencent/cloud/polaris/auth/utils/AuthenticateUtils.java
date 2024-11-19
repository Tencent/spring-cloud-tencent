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

package com.tencent.cloud.polaris.auth.utils;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for authenticate.
 *
 * @author Haotian Zhang
 */
public final class AuthenticateUtils {

	private static final Logger LOG = LoggerFactory.getLogger(AuthenticateUtils.class);

	private AuthenticateUtils() {

	}

	public static AuthResponse authenticate(AuthAPI authAPI, String namespace, String service, String path, String protocol, String method) {
		// build auth request
		AuthRequest authRequest = new AuthRequest(namespace, service, path, protocol, method, MetadataContextHolder.get());

		try {
			return authAPI.authenticate(authRequest);
		}
		catch (Throwable throwable) {
			LOG.error("fail to invoke authenticate of AuthAPI with AuthRequest[{}].", authRequest, throwable);
			return new AuthResponse(new AuthResult(AuthResult.Code.AuthResultOk));
		}
	}
}
