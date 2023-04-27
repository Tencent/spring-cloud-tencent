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

package com.tencent.cloud.polaris.circuitbreaker.common;

import com.tencent.cloud.common.metadata.MetadataContext;

import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;

/**
 * PolarisCircuitBreakerConfigBuilder.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerConfigBuilder implements ConfigBuilder<PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration> {

	private String namespace = MetadataContext.LOCAL_NAMESPACE;

	private String service;

	private String method;

	public PolarisCircuitBreakerConfigBuilder() {

	}

	public PolarisCircuitBreakerConfigBuilder(String namespace, String service, String method) {
		this.namespace = namespace;
		this.service = service;
		this.method = method;
	}

	public PolarisCircuitBreakerConfigBuilder namespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public PolarisCircuitBreakerConfigBuilder service(String service) {
		this.service = service;
		return this;
	}

	public PolarisCircuitBreakerConfigBuilder method(String method) {
		this.method = method;
		return this;
	}

	@Override
	public PolarisCircuitBreakerConfiguration build() {
		PolarisCircuitBreakerConfiguration conf = new PolarisCircuitBreakerConfiguration();
		conf.setNamespace(namespace);
		conf.setService(service);
		conf.setMethod(method);
		return conf;
	}

	public static class PolarisCircuitBreakerConfiguration {

		private final String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;

		private final String sourceService = MetadataContext.LOCAL_SERVICE;

		private String namespace;

		private String service;

		private String method;

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public String getService() {
			return service;
		}

		public void setService(String service) {
			this.service = service;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getSourceNamespace() {
			return sourceNamespace;
		}

		public String getSourceService() {
			return sourceService;
		}
	}

}
