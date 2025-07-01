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

package com.tencent.cloud.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kysonli
 */
public final class GzipUtil {

	private static final Logger LOG = LoggerFactory.getLogger(GzipUtil.class);

	private GzipUtil() {
	}

	public static byte[] compress(String data, String charsetName) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(data.getBytes(charsetName));
			gzip.finish();
			return bos.toByteArray();
		}
		catch (IOException e) {
			LOG.error("compress data [{}] error", data, e);
			throw e;
		}
	}

	public static String compressBase64Encode(String data, String charsetName) throws IOException {
		byte[] compressData = compress(data, charsetName);
		return new String(Base64.getEncoder().encode(compressData), charsetName);
	}

	public static String compressBase64Encode(byte[] byteData, String charsetName) throws IOException {
		byte[] compressData = compress(new String(byteData, charsetName), charsetName);
		return Base64.getEncoder().encodeToString(compressData);
	}


	public static byte[] decompress(byte[] zipData) throws IOException {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(zipData); GZIPInputStream gzip = new GZIPInputStream(bis); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] buf = new byte[256];
			int num;
			while ((num = gzip.read(buf)) != -1) {
				bos.write(buf, 0, num);
			}
			bos.flush();
			return bos.toByteArray();
		}
		catch (IOException e) {
			LOG.error("decompress zip data error", e);
			throw e;
		}
	}

	public static String base64DecodeDecompress(String data, String charsetName) throws IOException {
		byte[] base64DecodeData = Base64.getDecoder().decode(data);
		return new String(decompress(base64DecodeData), charsetName);
	}

	public static String base64DecodeDecompress(String data) throws IOException {
		return base64DecodeDecompress(data, "utf-8");
	}

}
