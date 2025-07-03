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

package com.tencent.tsf.gateway.core.util;

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

/**
 * @author kysonli
 * 2019/2/26 16:20
 */
public final class IdGenerator {

	private IdGenerator() {
	}

	public static String uuid() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}

	public static String generate() {
		UUID uuid = UUID.randomUUID();
		return compressedUUID(uuid);
	}

	public static String generateId(String prefix) {
		return generateId(prefix, '#');
	}

	public static String generateId(String prefix, char splitor) {
		String uuid = generate();
		return prefix + splitor + uuid;
	}

	private static String compressedUUID(UUID uuid) {
		byte[] byUuid = new byte[16];
		long least = uuid.getLeastSignificantBits();
		long most = uuid.getMostSignificantBits();
		long2bytes(most, byUuid, 0);
		long2bytes(least, byUuid, 8);
		return Base64.encodeBase64URLSafeString(byUuid);
	}

	private static void long2bytes(long value, byte[] bytes, int offset) {
		for (int i = 7; i > -1; --i) {
			bytes[offset++] = (byte) ((int) (value >> 8 * i & 255L));
		}

	}

	private static String compress(String uuidString) {
		UUID uuid = UUID.fromString(uuidString);
		return compressedUUID(uuid);
	}

	private static String uncompress(String compressedUuid) {
		if (compressedUuid.length() != 22) {
			throw new IllegalArgumentException("Invalid uuid!");
		}
		else {
			byte[] byUuid = Base64.decodeBase64(compressedUuid + "==");
			long most = bytes2long(byUuid, 0);
			long least = bytes2long(byUuid, 8);
			UUID uuid = new UUID(most, least);
			return uuid.toString();
		}
	}

	private static long bytes2long(byte[] bytes, int offset) {
		long value = 0L;

		for (int i = 7; i > -1; --i) {
			value |= ((long) bytes[offset++] & 255L) << 8 * i;
		}

		return value;
	}
}
