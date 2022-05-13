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
 *
 */

package com.tencent.cloud.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

/**
 * Read file content from classpath resource.
 *
 * @author lepdou 2022-04-20
 */
public final class ResourceFileUtils {

	private ResourceFileUtils() {
	}

	public static String readFile(String path) throws IOException {
		StringBuilder sb = new StringBuilder();

		ClassPathResource classPathResource = new ClassPathResource(path);

		if (classPathResource.exists() && classPathResource.isReadable()) {
			try (InputStream inputStream = classPathResource.getInputStream()) {
				byte[] buffer = new byte[1024 * 10];
				int len;
				while ((len = inputStream.read(buffer)) != -1) {
					sb.append(new String(buffer, 0, len, StandardCharsets.UTF_8));
				}
			}
		}
		return sb.toString();
	}

}
