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

package com.tencent.tsf.gateway.core.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.tencent.crypto.provider.SMCSProvider;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 国密加解密工具类.
 * @author xfenggeng
 * @date 2021-10-11 9:53
 */
public final class SM3Util {

	private static final String SM3_ALGORITHM = "SM3";
	private static final String SM3_ALGORITHM_HMAC = "SM3HMac";
	private static final String SMCS_PROVIDER = "SMCSProvider";
	private static final Logger logger = LoggerFactory.getLogger(SM3Util.class);

	static {
		try {
			Security.addProvider(new SMCSProvider());
		}
		catch (Throwable t) {
			logger.warn("load SMCSProvider error:{}", t.getMessage());
		}
	}

	private SM3Util() {

	}

	/**
	 * 计算SM3摘要.
	 * @param content 原文
	 * @return 摘要
	 */
	public static byte[] hmacSm3(String secretKey, String content) {
		try {
			// Get Mac instance
			Mac sm3HMac = Mac.getInstance(SM3_ALGORITHM_HMAC, SMCS_PROVIDER);
			// Initialize Mac with a specified key
			SecretKeySpec keySpec = new SecretKeySpec(StringUtils.getBytesUtf8(secretKey), SM3_ALGORITHM);
			sm3HMac.init(keySpec);
			// Generate MAC for the specified message
			byte[] mac = sm3HMac.doFinal(StringUtils.getBytesUtf8(content));
			return mac;
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
		catch (NoSuchProviderException e) {
			throw new IllegalArgumentException(e);
		}
		catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
