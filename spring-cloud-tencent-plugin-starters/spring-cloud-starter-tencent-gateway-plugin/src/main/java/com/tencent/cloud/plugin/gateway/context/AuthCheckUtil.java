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

package com.tencent.cloud.plugin.gateway.context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.constant.AuthMode;
import com.tencent.tsf.gateway.core.constant.CommonStatus;
import com.tencent.tsf.gateway.core.constant.HeaderName;
import com.tencent.tsf.gateway.core.constant.TsfAlgType;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.util.TsfSignUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.server.ServerWebExchange;

public final class AuthCheckUtil {

	private static final Logger logger = LoggerFactory.getLogger(AuthCheckUtil.class);

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private AuthCheckUtil() {
	}

	public static void signCheck(ServerWebExchange exchange, GroupContext groupContext) {
		AuthMode authMode = Optional.ofNullable(groupContext.getAuth()).map(GroupContext.ContextAuth::getType)
				.orElse(null);
		if (!AuthMode.SECRET.equals(authMode)) {
			return;
		}

		String secretId = exchange.getRequest().getHeaders().getFirst(HeaderName.APP_KEY);
		if (StringUtils.isEmpty(secretId)) {
			logger.error("[signCheck] secretId is empty. path: {}", exchange.getRequest().getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_BAD_REQUEST, "RequestHeader x-mg-secretid is required");
		}
		String nonce = exchange.getRequest().getHeaders().getFirst(HeaderName.NONCE);
		if (StringUtils.isEmpty(nonce)) {
			logger.error("[signCheck] nonce is empty. path: {}", exchange.getRequest().getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_BAD_REQUEST, "RequestHeader x-mg-nonce is required");
		}
		String alg = exchange.getRequest().getHeaders().getFirst(HeaderName.ALG);
		if (StringUtils.isEmpty(alg)) {
			logger.error("[signCheck] alg is empty. path: {}", exchange.getRequest().getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_BAD_REQUEST, "RequestHeader x-mg-alg is required");
		}
		String clientSign = exchange.getRequest().getHeaders().getFirst(HeaderName.SIGN);
		if (StringUtils.isEmpty(clientSign)) {
			logger.error("[signCheck] sign is empty. path: {}", exchange.getRequest().getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "RequestHeader x-mg-sign is wrong");
		}

		TsfAlgType algType = TsfAlgType.getSecurityCode(alg);

		List<GroupContext.ContextSecret> secretList = groupContext.getAuth().getSecrets();

		GroupContext.ContextSecret secret = null;
		if (secretList != null) {
			for (GroupContext.ContextSecret s : secretList) {
				if (s.getId().equals(secretId)) {
					secret = s;
					break;
				}
			}
		}

		if (secret == null) {
			logger.error("[signCheck] secret is not found. secretId:{}, path: {}", secretId, exchange.getRequest()
					.getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "GroupSecret Not Found");
		}
		if (CommonStatus.DISABLED.getStatus().equals(secret.getStatus())) {
			logger.error("[signCheck] secret is invalid. secretId:{}, path: {}", secretId, exchange.getRequest()
					.getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_BAD_REQUEST, "GroupSecret Invalid");
		}

		String expiredTime = secret.getExpiredTime();
		LocalDateTime expiredDateTime = LocalDateTime.parse(expiredTime, FORMATTER);
		LocalDateTime nowDateTime = LocalDateTime.now();
		boolean expired = nowDateTime.isAfter(expiredDateTime);

		if (expired) {
			logger.error("[signCheck] secret is expired. secretId:{}, path: {}", secretId, exchange.getRequest()
					.getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "GroupSecret Expired");
		}


		String serverSign = TsfSignUtil.generate(nonce, secretId, secret.getKey(), algType);
		if (!StringUtils.equals(clientSign, serverSign)) {
			logger.error("[signCheck] sign check failed. secretId:{}, nonce:{}, secretKey:{}, clientSign:{}, path: {}",
					secretId, nonce, secret.getKey(), clientSign, exchange.getRequest().getURI().getPath());
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "GroupSecret Check Failed");
		}
	}
}
