/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.context;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Because polaris-context is initialized in the bootstrap phase, the initialization of
 * InetUtilsProperties is required. The impact on user usage is that
 * spring.cloud.inetutils.defaultIpAddress needs to be configured in bootstrap.yml to take
 * effect.
 *
 * @see org.springframework.cloud.commons.util.InetUtilsProperties
 */
@ConfigurationProperties(InetUtilsProperties.PREFIX)
public class InetUtilsProperties {

	/**
	 * Prefix for the Inet Utils properties.
	 */
	public static final String PREFIX = "spring.cloud.inetutils";

	/**
	 * The default IP address. Used in case of errors.
	 */
	private String defaultIpAddress = "127.0.0.1";

	String getDefaultIpAddress() {
		return defaultIpAddress;
	}

	void setDefaultIpAddress(String defaultIpAddress) {
		this.defaultIpAddress = defaultIpAddress;
	}

}
