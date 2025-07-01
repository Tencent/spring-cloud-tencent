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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

public class EncryptAlgorithm {
	public static class AES256 {

		/**
		 * 加密.
		 *
		 * @param content 明文
		 * @param password 密钥
		 * @return 密文
		 */
		public static final String encrypt(String content, String password) {
			if (null == password || "".equals(password)) {
				throw new PasswordNotFoundException();
			}
			try {
				// AES SK生成器
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
				// SHA-256摘要密钥后生成安全随机数
				SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
				sr.setSeed(SHA256.encode(password));
				kgen.init(256, sr);
				// 生成秘密（对称）密钥
				SecretKey secretKey = kgen.generateKey();
				// 返回基本编码格式的密钥
				byte[] enCodeFormat = secretKey.getEncoded();
				// 根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；"AES"：与给定的密钥内容相关联的密钥算法的名称
				SecretKeySpec skSpec = new SecretKeySpec(enCodeFormat, "AES");
				// 将提供程序添加到下一个可用位置
				Security.addProvider(new BouncyCastleProvider());
				// 创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
				// "AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
				Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
				// 初始化cipher：加密模式
				cipher.init(Cipher.ENCRYPT_MODE, skSpec);
				byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
				byte[] cryptograph = cipher.doFinal(byteContent);
				byte[] enryptedContent = Base64.encode(cryptograph);
				return new String(enryptedContent);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed encrypt.", e);
			}
		}

		/**
		 * 解密.
		 *
		 * @param encryptedContent 密文
		 * @param password 密钥
		 * @return 明文
		 */
		public static final String decrypt(String encryptedContent, String password) {
			if (null == password || "".equals(password)) {
				throw new PasswordNotFoundException();
			}
			try {
				// AES SK生成器
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
				// SHA-256摘要密钥后生成安全随机数
				SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
				sr.setSeed(SHA256.encode(password));
				kgen.init(256, sr);
				// 生成秘密（对称）密钥
				SecretKey secretKey = kgen.generateKey();
				// 返回基本编码格式的密钥
				byte[] enCodeFormat = secretKey.getEncoded();
				// 根据给定的字节数组构造一个密钥。enCodeFormat：密钥内容；"AES"：与给定的密钥内容相关联的密钥算法的名称
				SecretKeySpec skSpec = new SecretKeySpec(enCodeFormat, "AES");
				// 将提供程序添加到下一个可用位置
				Security.addProvider(new BouncyCastleProvider());
				// 创建一个实现指定转换的 Cipher对象，该转换由指定的提供程序提供。
				// "AES/ECB/PKCS7Padding"：转换的名称；"BC"：提供程序的名称
				Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
				// 初始化cipher：解密模式
				cipher.init(Cipher.DECRYPT_MODE, skSpec);
				byte[] result = cipher.doFinal(Base64.decode(encryptedContent.getBytes(StandardCharsets.UTF_8)));
				return new String(result);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed decrypt.", e);
			}
		}
	}

	public static class SHA256 {

		/**
		 * 计算SHA-256摘要.
		 *
		 * @param content 原文
		 * @return 摘要
		 * @throws NoSuchAlgorithmException 算法不存在时抛出
		 */
		public static byte[] encode(String content) throws NoSuchAlgorithmException {
			MessageDigest digester = MessageDigest.getInstance("SHA-256");
			digester.update(content.getBytes(StandardCharsets.UTF_8));
			return digester.digest();
		}
	}

	public static class PasswordNotFoundException extends RuntimeException {

		/**
		 * serialVersionUID.
		 */
		private static final long serialVersionUID = -2843758461182470411L;

		public PasswordNotFoundException() {
			super("Password not found.");
		}

	}
}
