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

package com.tencent.cloud.common.util;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author heihuliliu
 */
public final class FeignUtil {

	/**
	 * Feign client spec.
	 */
	public static final String FEIGN_CLIENT_SPECIF = ".FeignClientSpecification";

	/**
	 * Default Feign client spec.
	 */
	public static final String FEIGN_CLIENT_DEFAULT = "default.";

	/**
	 * regular expression that parses ${xxx} .
	 */
	public static final String REGEX = "^[$][{](.*)[}]$";

	/**
	 * replacement of ${xxx}.
	 */
	public static final String REPLACEMENT = "$1";

	private FeignUtil() {

	}

	/**
	 * TODO If @FeignClient specifies contextId, the service name will not be obtained correctly, but the contextId will be obtained.
	 *
	 * @param name feign name.
	 * @param context application context.
	 * @return service name.
	 */
	public static String analysisFeignName(String name, ApplicationContext context) {
		String feignName = "";
		String feignPath = name.substring(0, name.indexOf(FEIGN_CLIENT_SPECIF));
		// Handle the case where the service name is a variable
		if (feignPath.matches(REGEX)) {
			feignPath = context.getEnvironment().getProperty(feignPath.replaceAll(REGEX, REPLACEMENT));
		}
		if (StringUtils.hasText(feignPath)) {
			// The case of multi-level paths
			String[] feignNames = feignPath.split("/");
			if (feignNames.length > 1) {
				for (int i = 0; i < feignNames.length; i++) {
					if (StringUtils.hasText(feignNames[i])) {
						feignName = feignNames[i];
						break;
					}
				}
			}
			else {
				feignName = feignNames[0];
			}
		}
		return feignName;
	}
}
