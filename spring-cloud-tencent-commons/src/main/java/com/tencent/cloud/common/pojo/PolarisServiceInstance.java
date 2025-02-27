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

package com.tencent.cloud.common.pojo;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Polaris's implementation of {@link ServiceInstance}.
 *
 * @author Haotian Zhang, changjin wei(魏昌进)
 */
public class PolarisServiceInstance implements ServiceInstance {

	private final Instance instance;

	private final boolean isSecure;

	private final String scheme;

	private final Map<String, String> serviceMetadata;

	public PolarisServiceInstance(Instance instance) {
		this(instance, null);
	}

	public PolarisServiceInstance(Instance instance, Map<String, String> metadata) {
		this.instance = instance;
		this.isSecure = StringUtils.equalsIgnoreCase(instance.getProtocol(), "https");
		if (isSecure) {
			scheme = "https";
		}
		else {
			scheme = "http";
		}
		this.serviceMetadata = new HashMap<>();
		if (CollectionUtils.isNotEmpty(metadata)) {
			this.serviceMetadata.putAll(metadata);
		}
	}

	public Instance getPolarisInstance() {
		return instance;
	}

	@Override
	public String getInstanceId() {
		return instance.getId();
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

	public Map<String, String> getServiceMetadata() {
		return serviceMetadata;
	}

	/**
	 * To fix loadbalancer not working bug when importing spring-retry.
	 * @param o object
	 * @return if equals
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PolarisServiceInstance that = (PolarisServiceInstance) o;
		return Objects.equals(instance, that.instance) && Objects.equals(scheme, that.scheme);
	}

	@Override
	public int hashCode() {
		return Objects.hash(instance, scheme);
	}

	@Override
	public String toString() {
		return "PolarisServiceInstance{" +
				"instance=" + instance +
				", isSecure=" + isSecure +
				", scheme='" + scheme + '\'' +
				", serviceMetadata=" + serviceMetadata +
				'}';
	}
}
