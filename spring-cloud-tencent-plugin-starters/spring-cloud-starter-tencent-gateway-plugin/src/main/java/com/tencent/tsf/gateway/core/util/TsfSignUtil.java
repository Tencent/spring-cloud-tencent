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

import com.tencent.tsf.gateway.core.constant.TsfAlgType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author kysonli
 * 2019/2/26 16:06
 */
public final class TsfSignUtil {
	private static final Logger logger = LoggerFactory.getLogger(TsfSignUtil.class);


	private TsfSignUtil() {
	}

	/**
	 * 生成签名.
	 *
	 * @param nonce 随机字符串
	 * @param secretId 秘钥ID
	 * @param secretKey 秘钥值
	 * @param algType 签名算法 {@link TsfAlgType}
	 */
	@SuppressWarnings("deprecation")
	public static String generate(String nonce, String secretId, String secretKey, TsfAlgType algType) {
		String digestValue = nonce + secretId + secretKey;
		byte[] serverSignBytes;
		switch (algType) {
		case HMAC_MD5:
			serverSignBytes = HmacUtils.hmacMd5(secretKey, digestValue);
			break;
		case HMAC_SHA_1:
			serverSignBytes = HmacUtils.hmacSha1(secretKey, digestValue);
			break;
		case HMAC_SHA_256:
			serverSignBytes = HmacUtils.hmacSha256(secretKey, digestValue);
			break;
		case HMAC_SHA_512:
			serverSignBytes = HmacUtils.hmacSha512(secretKey, digestValue);
			break;
		case HMAC_SM3:
			serverSignBytes = SM3Util.hmacSm3(secretKey, digestValue);
			break;
		default:
			throw new UnsupportedOperationException("不支持的鉴权算法: " + algType);
		}
		String signValue = Base64.encodeBase64String(serverSignBytes);
		if (logger.isDebugEnabled()) {
			logger.debug("签名明文：{}，签名密文：{}", digestValue, signValue);
		}
		return signValue;
	}

}
