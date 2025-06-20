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

package com.tencent.tsf.gateway.core.plugin;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.gateway.context.Position;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.model.ClaimMapping;
import com.tencent.tsf.gateway.core.model.JwtPlugin;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @ClassName JwtGatewayPlugin
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/8 16:45
 * @Version 1.0
 */
public class JwtGatewayPlugin implements IGatewayPlugin<JwtPlugin> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public PluginPayload startUp(JwtPlugin pluginInfo, TsfGatewayRequest tsfGatewayRequest) {
		Map<String, String[]> parameterMap = tsfGatewayRequest.getParameterMap();
		String tokenBaggagePosition = pluginInfo.getTokenBaggagePosition();
		if (Position.fromString(tokenBaggagePosition) == null) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "tokenBaggagePosition is wrong");
		}
		String idToken;
		if (Position.HEADER.equals(Position.fromString(tokenBaggagePosition))) {
			idToken = tsfGatewayRequest.getRequestHeader(pluginInfo.getTokenKeyName());
		}
		else {
			//queryParam中取
			if (parameterMap.get(pluginInfo.getTokenKeyName()) == null || parameterMap.get(pluginInfo.getTokenKeyName()).length == 0) {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "idToken is empty");
			}
			idToken = parameterMap.get(pluginInfo.getTokenKeyName())[0];
		}
		if (StringUtils.isEmpty(idToken)) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "idToken is empty");
		}

		//获取公钥
		String publicKeyJson = pluginInfo.getPublicKeyJson();
		PublicJsonWebKey jwk;
		//验签
		try {
			jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKeyJson);
			jwk.setKeyId(pluginInfo.getKid());
		}
		catch (JoseException e) {
			logger.error("Generate PublicKey Error", e);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "Generate PublicKey Error");
		}
		//解析idToken, 验签
		JwtConsumer jwtConsumer = new JwtConsumerBuilder()
				.setRequireExpirationTime() // the JWT must have an expiration time
				.setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
				.setRequireSubject() // the JWT must have a subject claim
				.setVerificationKey(jwk.getPublicKey())
				.setJwsAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, jwk.getAlgorithm()))
				// ignore audience
				.setSkipDefaultAudienceValidation()
				.build(); // create the JwtConsumer instance

		JwtClaims jwtClaims;
		try {
			jwtClaims = jwtConsumer.processToClaims(idToken);
		}
		catch (InvalidJwtException e) {
			// Whether or not the JWT has expired being one common reason for invalidity
			if (e.hasExpired()) {
				try {
					long expirationTime = e.getJwtContext()
							.getJwtClaims()
							.getExpirationTime()
							.getValueInMillis();
					String msg = String
							.format("JWT expired at (%d)", expirationTime);
					logger.info(msg);
					throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, msg);
				}
				catch (MalformedClaimException e1) {
					// ignore
				}
			}

			logger.warn("Invalid JWT! ", e);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "Invalid JWT");
		}

		if (jwtClaims == null) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "Invalid JWT");
		}

		PluginPayload pluginPayload = new PluginPayload();
		//若成功,则封装claim字段映射到后台服务
		if (StringUtils.isNotEmpty(pluginInfo.getClaimMappingJson())) {
			logger.info("claim json is : " + pluginInfo.getClaimMappingJson());
			List<ClaimMapping> claimMappings;
			try {
				claimMappings = JacksonUtils.deserializeCollection(pluginInfo.getClaimMappingJson(), ClaimMapping.class);
			}
			catch (Throwable t) {
				logger.error("[tsf-gateway] claimMappingJson deserialize data to claimMappings occur exception.", t);
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_SERIALIZE_ERROR,
						"claimMappingJson deserialize data to claimMappings occur exception");
			}

			Map<String, String[]> paramsMap = new HashMap<>();
			Map<String, String> headerParamsMap = new HashMap<>();
			for (ClaimMapping claimMapping : claimMappings) {
				try {
					String claimValue = jwtClaims.getStringClaimValue(claimMapping.getParameterName());
					if (!StringUtils.isEmpty(claimValue)) {
						if (Position.HEADER.equals(Position.fromString(claimMapping.getLocation()))) {
							headerParamsMap.put(claimMapping.getMappingParameterName(), claimValue);
						}
						else if (Position.QUERY.equals(Position.fromString(claimMapping.getLocation()))) {
							paramsMap.put(claimMapping.getMappingParameterName(), new String[] {claimValue});
						}
					}
				}
				catch (MalformedClaimException e) {
					logger.warn("Claim mapping error, parameterName: " + claimMapping.getParameterName(), e);
				}
			}
			pluginPayload.setRequestHeaders(headerParamsMap);
			pluginPayload.setParameterMap(paramsMap);
		}

		return pluginPayload;
	}

}
