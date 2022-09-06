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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Metadata Context.
 *
 * @author Haotian Zhang
 */
public class MetadataContext {

	/**
	 * transitive context.
	 */
	public static final String FRAGMENT_TRANSITIVE = "transitive";

	/**
	 * disposable Context.
	 */
	public static final String FRAGMENT_DISPOSABLE = "disposable";

	/**
	 * load balancer context.
	 */
	public static final String FRAGMENT_LOAD_BALANCER = "loadbalancer";

	/**
	 * upstream disposable Context.
	 */
	public static final String FRAGMENT_UPSTREAM_DISPOSABLE = "upstream-disposable";

	private static final Logger LOG = LoggerFactory.getLogger(MetadataContext.class);
	/**
	 * Namespace of local instance.
	 */
	public static String LOCAL_NAMESPACE;

	/**
	 * Service name of local instance.
	 */
	public static String LOCAL_SERVICE;

	static {
		String namespace = ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.namespace");
		if (StringUtils.isEmpty(namespace)) {
			namespace = ApplicationContextAwareUtils
					.getProperties("spring.cloud.polaris.discovery.namespace", "default");
		}
		if (StringUtils.isEmpty(namespace)) {
			LOG.error("namespace should not be blank. please configure spring.cloud.polaris.namespace or "
					+ "spring.cloud.polaris.discovery.namespace");
			throw new RuntimeException("namespace should not be blank. please configure spring.cloud.polaris.namespace or "
					+ "spring.cloud.polaris.discovery.namespace");
		}
		LOCAL_NAMESPACE = namespace;

		String serviceName = ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.service");
		if (StringUtils.isEmpty(serviceName)) {
			serviceName = ApplicationContextAwareUtils.getProperties(
					"spring.cloud.polaris.discovery.service", ApplicationContextAwareUtils
							.getProperties("spring.application.name", null));
		}
		if (StringUtils.isEmpty(serviceName)) {
			LOG.error("service name should not be blank. please configure spring.cloud.polaris.service or "
					+ "spring.cloud.polaris.discovery.service or spring.application.name");
			throw new RuntimeException("service name should not be blank. please configure spring.cloud.polaris.service or "
					+ "spring.cloud.polaris.discovery.service or spring.application.name");
		}
		LOCAL_SERVICE = serviceName;
	}

	private final Map<String, Map<String, String>> fragmentContexts;

	public MetadataContext() {
		this.fragmentContexts = new ConcurrentHashMap<>();
	}

	public Map<String, String> getFragmentContext(String fragment) {
		Map<String, String> fragmentContext = fragmentContexts.get(fragment);
		if (fragmentContext == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(fragmentContext);
	}

	public String getContext(String fragment, String key) {
		Map<String, String> fragmentContext = fragmentContexts.get(fragment);
		if (fragmentContext == null) {
			return null;
		}
		return fragmentContext.get(key);
	}

	public void putContext(String fragment, String key, String value) {
		Map<String, String> fragmentContext = fragmentContexts.get(fragment);
		if (fragmentContext == null) {
			fragmentContext = new ConcurrentHashMap<>();
			fragmentContexts.put(fragment, fragmentContext);
		}
		fragmentContext.put(key, value);
	}

	public void putFragmentContext(String fragment, Map<String, String> context) {
		fragmentContexts.put(fragment, context);
	}

	@Override
	public String toString() {
		return "MetadataContext{" +
				"fragmentContexts=" + JacksonUtils.serialize2Json(fragmentContexts) +
				'}';
	}
}
