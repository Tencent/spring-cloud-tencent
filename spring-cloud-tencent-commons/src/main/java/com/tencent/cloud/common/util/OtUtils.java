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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for otel.
 *
 * @author Haotian Zhang
 */
public final class OtUtils {

	/**
	 * Key of otel resource attributes.
	 */
	public static final String OTEL_RESOURCE_ATTRIBUTES = "otel.resource.attributes";

	/**
	 * Key of lane id.
	 */
	public static final String OTEL_LANE_ID_KEY = "lane-id";

	/**
	 * Key of prefer ipv6.
	 */
	public static final String OTEL_PREFER_IPV6_KEY = "prefer.ipv6";

	private static final Logger LOGGER = LoggerFactory.getLogger(OtUtils.class);

	private static String otServiceName = null;

	private OtUtils() {
	}

	/**
	 * If the service name is not set, it will be set when the service is registered.
	 *
	 * @param serviceName service name
	 */
	public static void setOtServiceNameIfNeeded(String serviceName) {
		try {
			String attributes = null;
			if (null != System.getenv(OTEL_RESOURCE_ATTRIBUTES)) {
				attributes = System.getenv(OTEL_RESOURCE_ATTRIBUTES);
			}
			if (null != System.getProperty(OTEL_RESOURCE_ATTRIBUTES)) {
				attributes = System.getProperty(OTEL_RESOURCE_ATTRIBUTES);
			}
			if (attributes == null || !attributes.contains("service.name")) {
				otServiceName = serviceName;
				System.setProperty(OTEL_RESOURCE_ATTRIBUTES, "service.name=" + serviceName);
				LOGGER.info("update ot service name, old:{}, new:{}",
						attributes, System.getProperty(OTEL_RESOURCE_ATTRIBUTES));
			}
			else {
				for (String attribute : attributes.split(",")) {
					if (attribute.contains("service.name=")) {
						otServiceName = attribute.replace("service.name=", "");
						LOGGER.info("use env ot service name:{}", otServiceName);
					}
				}
			}
		}
		catch (Throwable throwable) {
			LOGGER.error("set ot service name failed.", throwable);
		}
	}

	public static String getOtServiceName() {
		return otServiceName;
	}
}
