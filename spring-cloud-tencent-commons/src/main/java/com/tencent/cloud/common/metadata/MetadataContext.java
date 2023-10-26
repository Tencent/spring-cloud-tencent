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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.DiscoveryUtil;
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
	 * upstream disposable Context.
	 */
	public static final String FRAGMENT_UPSTREAM_DISPOSABLE = "upstream-disposable";

	/**
	 * the key of the header(key) list needed to be transmitted from upstream to downstream.
	 */
	public static final String FRAGMENT_RAW_TRANSHEADERS = "trans-headers";

	/**
	 * the key of the header(key-value) list needed to be transmitted from upstream to downstream.
	 */
	public static final String FRAGMENT_RAW_TRANSHEADERS_KV = "trans-headers-kv";

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
		String namespace = ApplicationContextAwareUtils
				.getProperties("spring.cloud.polaris.namespace");
		if (!StringUtils.hasText(namespace)) {
			namespace = ApplicationContextAwareUtils
					.getProperties("spring.cloud.polaris.discovery.namespace", "default");
		}

		if (!StringUtils.hasText(namespace)) {
			LOG.error("namespace should not be blank. please configure spring.cloud.polaris.namespace or "
					+ "spring.cloud.polaris.discovery.namespace");
			throw new RuntimeException("namespace should not be blank. please configure spring.cloud.polaris.namespace or "
					+ "spring.cloud.polaris.discovery.namespace");
		}
		namespace = DiscoveryUtil.rewriteNamespace(namespace);
		LOCAL_NAMESPACE = namespace;

		String serviceName = ApplicationContextAwareUtils
				.getProperties("spring.cloud.polaris.service");
		if (!StringUtils.hasText(serviceName)) {
			serviceName = ApplicationContextAwareUtils.getProperties(
					"spring.cloud.polaris.discovery.service", ApplicationContextAwareUtils
							.getProperties("spring.application.name", null));
		}
		if (!StringUtils.hasText(serviceName)) {
			LOG.error("service name should not be blank. please configure spring.cloud.polaris.service or "
					+ "spring.cloud.polaris.discovery.service or spring.application.name");
			throw new RuntimeException("service name should not be blank. please configure spring.cloud.polaris.service or "
					+ "spring.cloud.polaris.discovery.service or spring.application.name");
		}
		serviceName = DiscoveryUtil.rewriteServiceId(serviceName);
		LOCAL_SERVICE = serviceName;
	}

	private final Map<String, Map<String, String>> fragmentContexts;

	private final Map<String, Object> loadbalancerMetadata;


	public MetadataContext() {
		this.fragmentContexts = new ConcurrentHashMap<>();
		this.loadbalancerMetadata = new ConcurrentHashMap<>();
	}

	public Map<String, String> getDisposableMetadata() {
		return this.getFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE);
	}

	public Map<String, String> getTransitiveMetadata() {
		return this.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
	}

	public Map<String, String> getCustomMetadata() {
		Map<String, String> transitiveMetadata = this.getTransitiveMetadata();
		Map<String, String> disposableMetadata = this.getDisposableMetadata();
		Map<String, String> customMetadata = new HashMap<>();
		// Clean up one-time metadata coming from upstream .
		transitiveMetadata.forEach((key, value) -> {
			if (!disposableMetadata.containsKey(key)) {
				customMetadata.put(key, value);
			}
		});
		return Collections.unmodifiableMap(customMetadata);
	}

	public Map<String, String> getTransHeaders() {
		return this.getFragmentContext(MetadataContext.FRAGMENT_RAW_TRANSHEADERS);
	}

	public Map<String, String> getTransHeadersKV() {
		return this.getFragmentContext(MetadataContext.FRAGMENT_RAW_TRANSHEADERS_KV);
	}

	public Map<String, Object> getLoadbalancerMetadata() {
		return this.loadbalancerMetadata;
	}

	public void setTransitiveMetadata(Map<String, String> transitiveMetadata) {
		this.putFragmentContext(FRAGMENT_TRANSITIVE, Collections.unmodifiableMap(transitiveMetadata));
	}

	public void setDisposableMetadata(Map<String, String> disposableMetadata) {
		this.putFragmentContext(FRAGMENT_DISPOSABLE, Collections.unmodifiableMap(disposableMetadata));
	}

	public void setUpstreamDisposableMetadata(Map<String, String> upstreamDisposableMetadata) {
		this.putFragmentContext(FRAGMENT_UPSTREAM_DISPOSABLE, Collections.unmodifiableMap(upstreamDisposableMetadata));
	}

	public void setTransHeadersKV(String key, String value) {
		this.putContext(FRAGMENT_RAW_TRANSHEADERS_KV, key, value);
	}

	public void setTransHeaders(String key, String value) {
		this.putContext(FRAGMENT_RAW_TRANSHEADERS, key, value);
	}

	public void setLoadbalancer(String key, Object value) {
		this.loadbalancerMetadata.put(key, value);
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
