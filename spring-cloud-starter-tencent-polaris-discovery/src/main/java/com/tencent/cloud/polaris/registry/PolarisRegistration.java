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
 */

package com.tencent.cloud.polaris.registry;

import java.net.URI;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.polaris.PolarisProperties;
import com.tencent.polaris.client.api.SDKContext;
import org.apache.commons.lang.StringUtils;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * Registration object of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisRegistration implements Registration, ServiceInstance {

	private final PolarisProperties polarisProperties;

	private final SDKContext polarisContext;

	public PolarisRegistration(PolarisProperties polarisProperties, SDKContext context) {
		this.polarisProperties = polarisProperties;
		this.polarisContext = context;
	}

	@Override
	public String getServiceId() {
		return polarisProperties.getService();
	}

	@Override
	public String getHost() {
		if (StringUtils.isNotBlank(polarisProperties.getIpAddress())) {
			return polarisProperties.getIpAddress();
		}
		return polarisContext.getConfig().getGlobal().getAPI().getBindIP();
	}

	@Override
	public int getPort() {
		return polarisProperties.getPort();
	}

	public void setPort(int port) {
		this.polarisProperties.setPort(port);
	}

	@Override
	public boolean isSecure() {
		return StringUtils.equalsIgnoreCase(polarisProperties.getProtocol(), "https");
	}

	@Override
	public URI getUri() {
		return DefaultServiceInstance.getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return MetadataContextHolder.get().getAllSystemMetadata();
	}

	public PolarisProperties getPolarisProperties() {
		return polarisProperties;
	}

	@Override
	public String toString() {
		return "PolarisRegistration{" + "polarisProperties=" + polarisProperties
				+ ", polarisContext=" + polarisContext + '}';
	}

}
