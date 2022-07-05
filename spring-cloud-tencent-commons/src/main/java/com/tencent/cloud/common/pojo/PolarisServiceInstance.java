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

package com.tencent.cloud.common.pojo;

import java.net.URI;
import java.util.Map;

import com.tencent.polaris.api.pojo.Instance;
import org.apache.commons.lang.StringUtils;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Polaris's implementation of {@link ServiceInstance}.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceInstance implements ServiceInstance {

	private final Instance instance;

	private final boolean isSecure;

	private final String scheme;

	public PolarisServiceInstance(Instance instance) {
		this.instance = instance;
		this.isSecure = StringUtils.equalsIgnoreCase(instance.getProtocol(), "https");
		if (isSecure) {
			scheme = "https";
		}
		else {
			scheme = "http";
		}
	}

	public Instance getPolarisInstance() {
		return instance;
	}

	@Override
	public String getInstanceId() {
		return ServiceInstance.super.getInstanceId();
	}

	@Override
	public String getServiceId() {
		return instance.getService();
	}

	@Override
	public String getHost() {
		return instance.getHost();
	}

	@Override
	public int getPort() {
		return instance.getPort();
	}

	@Override
	public boolean isSecure() {
		return this.isSecure;
	}

	@Override
	public URI getUri() {
		return DefaultServiceInstance.getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return instance.getMetadata();
	}

	@Override
	public String getScheme() {
		return this.scheme;
	}
}
