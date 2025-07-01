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

package com.tencent.cloud.polaris.config.tsf.encrypt;

import org.springframework.util.StringUtils;

public final class EncryptConfig {

	private static final String PASSWORD_KEY = "tsf_config_encrypt_password";
	/**
	 * 加密前缀.
	 */
	public static String ENCRYPT_PREFIX = "ENC(";
	/**
	 * 加密后缀.
	 */
	public static String ENCRYPT_SUFFIX = ")";
	/**
	 * 密码.
	 */
	private static String password;
	/**
	 * 加解密提供器类名.
	 */
	private static String providerClass = "com.tencent.cloud.polaris.config.tsf.encrypt.ConfigEncryptAESProvider";

	static {
		// 环境变量
		if (null != System.getenv(PASSWORD_KEY)) {
			password = System.getenv(PASSWORD_KEY);
		}
		// JVM参数
		if (null != System.getProperty(PASSWORD_KEY)) {
			password = System.getProperty(PASSWORD_KEY);
		}
	}

	private EncryptConfig() {

	}

	/**
	 * 是否开启配置，判断 password 是否为空.
	 */
	public static Boolean getEnabled() {
		return !StringUtils.isEmpty(password);
	}

	public static String getPassword() {
		return EncryptConfig.password;
	}

	public static void setPassword(String password) {
		EncryptConfig.password = password;
	}

	public static ConfigEncryptProvider getProvider() {
		return ConfigEncryptProviderFactory.getInstance();
	}

	public static String getProviderClass() {
		return providerClass;
	}

	public static void setProviderClass(String providerClass) {
		EncryptConfig.providerClass = providerClass;
	}

	/**
	 * 是否需要进行解密.
	 *
	 * @param content 判断对象
	 * @return true：需要解密；false：不需要解密
	 */
	public static Boolean needDecrypt(Object content) {
		if (null == content) {
			return false;
		}
		else {
			String stringValue = String.valueOf(content);
			return stringValue.startsWith(ENCRYPT_PREFIX) && stringValue.endsWith(ENCRYPT_SUFFIX);
		}
	}

	/**
	 * 获取真实密文.
	 *
	 * @param content 原始配置值
	 * @return 真实密文
	 */
	public static String realContent(Object content) {
		if (null != content) {
			String stringValue = String.valueOf(content);
			return stringValue.substring(ENCRYPT_PREFIX.length(), stringValue.length() - ENCRYPT_SUFFIX.length());
		}
		return null;
	}

}
