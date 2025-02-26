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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.DiscoveryUtil;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataMapValue;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataStringValue;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.MetadataValue;
import com.tencent.polaris.metadata.core.TransitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

public class MetadataContext extends com.tencent.polaris.metadata.core.manager.MetadataContext {

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
	 * disposable Context.
	 */
	public static final String FRAGMENT_APPLICATION = "application";

	/**
	 * upstream disposable Context.
	 */
	public static final String FRAGMENT_UPSTREAM_APPLICATION = "upstream-application";

	/**
	 * the key of the header(key) list needed to be transmitted from upstream to downstream.
	 */
	public static final String FRAGMENT_RAW_TRANSHEADERS = "trans-headers";

	/**
	 * the key of the header(key-value) list needed to be transmitted from upstream to downstream.
	 */
	public static final String FRAGMENT_RAW_TRANSHEADERS_KV = "trans-headers-kv";

	/**
	 * the key of the header(key-value) list needed to be store as loadbalance data.
	 */
	public static final String FRAGMENT_LB_METADATA = "load-balance-metadata";

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
		}
		serviceName = DiscoveryUtil.rewriteServiceId(serviceName);
		LOCAL_SERVICE = serviceName;
	}

	public MetadataContext() {
		super(MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX);
	}

	private Map<String, String> getMetadataAsMap(MetadataType metadataType, TransitiveType transitiveType, boolean caller) {
		MetadataContainer metadataContainer = getMetadataContainer(metadataType, caller);
		Map<String, String> values = new HashMap<>();
		metadataContainer.iterateMetadataValues(new BiConsumer<String, MetadataValue>() {
			@Override
			public void accept(String s, MetadataValue metadataValue) {
				if (metadataValue instanceof MetadataStringValue) {
					MetadataStringValue metadataStringValue = (MetadataStringValue) metadataValue;
					if (metadataStringValue.getTransitiveType() == transitiveType) {
						values.put(s, metadataStringValue.getStringValue());
					}
				}
			}
		});
		return values;
	}

	public void putMetadataAsMap(MetadataType metadataType, TransitiveType transitiveType, boolean caller, Map<String, String> values) {
		MetadataContainer metadataContainer = getMetadataContainer(metadataType, caller);
		for (Map.Entry<String, String> entry : values.entrySet()) {
			metadataContainer.putMetadataStringValue(entry.getKey(), entry.getValue(), transitiveType);
		}
	}

	private Map<String, String> getMapMetadataAsMap(MetadataType metadataType, String mapKey, TransitiveType transitiveType, boolean caller) {
		MetadataContainer metadataContainer = getMetadataContainer(metadataType, caller);
		Map<String, String> values = new HashMap<>();
		MetadataValue metadataValue = metadataContainer.getMetadataValue(mapKey);
		if (!(metadataValue instanceof MetadataMapValue)) {
			return values;
		}
		MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
		metadataMapValue.iterateMapValues(new BiConsumer<String, MetadataValue>() {
			@Override
			public void accept(String s, MetadataValue metadataValue) {
				if (metadataValue instanceof MetadataStringValue) {
					MetadataStringValue metadataStringValue = (MetadataStringValue) metadataValue;
					if (metadataStringValue.getTransitiveType() == transitiveType) {
						values.put(s, metadataStringValue.getStringValue());
					}
				}
			}
		});
		return values;
	}

	private void putMapMetadataAsMap(MetadataType metadataType, String mapKey,
			TransitiveType transitiveType, boolean caller, Map<String, String> values) {
		MetadataContainer metadataContainer = getMetadataContainer(metadataType, caller);
		for (Map.Entry<String, String> entry : values.entrySet()) {
			metadataContainer.putMetadataMapValue(mapKey, entry.getKey(), entry.getValue(), transitiveType);
		}
	}

	public Map<String, String> getDisposableMetadata() {
		return getFragmentContext(FRAGMENT_DISPOSABLE);
	}

	public void setDisposableMetadata(Map<String, String> disposableMetadata) {
		putFragmentContext(FRAGMENT_DISPOSABLE, Collections.unmodifiableMap(disposableMetadata));
	}

	public Map<String, String> getTransitiveMetadata() {
		return getFragmentContext(FRAGMENT_TRANSITIVE);
	}

	public void setTransitiveMetadata(Map<String, String> transitiveMetadata) {
		putFragmentContext(FRAGMENT_TRANSITIVE, Collections.unmodifiableMap(transitiveMetadata));
	}

	public Map<String, String> getApplicationMetadata() {
		return getFragmentContext(FRAGMENT_APPLICATION);
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
		return this.getFragmentContext(FRAGMENT_RAW_TRANSHEADERS);
	}

	public Map<String, String> getTransHeadersKV() {
		return getFragmentContext(FRAGMENT_RAW_TRANSHEADERS_KV);
	}

	public Map<String, Object> getLoadbalancerMetadata() {
		MetadataContainer metadataContainer = getMetadataContainer(MetadataType.APPLICATION, false);
		MetadataValue metadataValue = metadataContainer.getMetadataValue(FRAGMENT_LB_METADATA);
		Map<String, Object> values = new HashMap<>();
		if (metadataValue instanceof MetadataMapValue) {
			MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
			metadataMapValue.iterateMapValues(new BiConsumer<String, MetadataValue>() {
				@Override
				public void accept(String s, MetadataValue metadataValue) {
					if (metadataValue instanceof MetadataObjectValue) {
						Optional<?> objectValue = ((MetadataObjectValue<?>) metadataValue).getObjectValue();
						objectValue.ifPresent(o -> values.put(s, o));
					}
				}
			});
		}
		return values;
	}

	public void setLoadbalancer(String key, Object value) {
		MetadataContainer metadataContainer = getMetadataContainer(MetadataType.APPLICATION, false);
		metadataContainer.putMetadataMapObjectValue(FRAGMENT_LB_METADATA, key, value);
	}

	public void setUpstreamDisposableMetadata(Map<String, String> upstreamDisposableMetadata) {
		putFragmentContext(FRAGMENT_UPSTREAM_DISPOSABLE, Collections.unmodifiableMap(upstreamDisposableMetadata));
	}

	public void setTransHeadersKV(String key, String value) {
		putContext(FRAGMENT_RAW_TRANSHEADERS_KV, key, value);
	}

	public void setTransHeaders(String key, String value) {
		putContext(FRAGMENT_RAW_TRANSHEADERS, key, value);
	}

	public Map<String, String> getFragmentContext(String fragment) {
		switch (fragment) {
		case FRAGMENT_TRANSITIVE:
			return getMetadataAsMap(MetadataType.CUSTOM, TransitiveType.PASS_THROUGH, false);
		case FRAGMENT_DISPOSABLE:
			return getMetadataAsMap(MetadataType.CUSTOM, TransitiveType.DISPOSABLE, false);
		case FRAGMENT_UPSTREAM_DISPOSABLE:
			return getMetadataAsMap(MetadataType.CUSTOM, TransitiveType.DISPOSABLE, true);
		case FRAGMENT_APPLICATION:
			return getMetadataAsMap(MetadataType.APPLICATION, TransitiveType.DISPOSABLE, false);
		case FRAGMENT_UPSTREAM_APPLICATION:
			return getMetadataAsMap(MetadataType.APPLICATION, TransitiveType.DISPOSABLE, true);
		case FRAGMENT_RAW_TRANSHEADERS:
			return getMapMetadataAsMap(MetadataType.CUSTOM, FRAGMENT_RAW_TRANSHEADERS, TransitiveType.NONE, false);
		case FRAGMENT_RAW_TRANSHEADERS_KV:
			return getMapMetadataAsMap(MetadataType.CUSTOM, FRAGMENT_RAW_TRANSHEADERS_KV, TransitiveType.NONE, false);
		default:
			return getMapMetadataAsMap(MetadataType.CUSTOM, fragment, TransitiveType.NONE, false);
		}
	}

	public String getContext(String fragment, String key) {
		Map<String, String> fragmentContext = getFragmentContext(fragment);
		if (fragmentContext == null) {
			return null;
		}
		return fragmentContext.get(key);
	}

	public void putContext(String fragment, String key, String value) {
		Map<String, String> values = new HashMap<>();
		values.put(key, value);
		putFragmentContext(fragment, values);
	}

	public void putFragmentContext(String fragment, Map<String, String> context) {
		switch (fragment) {
		case FRAGMENT_TRANSITIVE:
			putMetadataAsMap(MetadataType.CUSTOM, TransitiveType.PASS_THROUGH, false, context);
			break;
		case FRAGMENT_DISPOSABLE:
			putMetadataAsMap(MetadataType.CUSTOM, TransitiveType.DISPOSABLE, false, context);
			break;
		case FRAGMENT_UPSTREAM_DISPOSABLE:
			putMetadataAsMap(MetadataType.CUSTOM, TransitiveType.DISPOSABLE, true, context);
			break;
		case FRAGMENT_APPLICATION:
			putMetadataAsMap(MetadataType.APPLICATION, TransitiveType.DISPOSABLE, false, context);
			break;
		case FRAGMENT_UPSTREAM_APPLICATION:
			putMetadataAsMap(MetadataType.APPLICATION, TransitiveType.DISPOSABLE, true, context);
			break;
		case FRAGMENT_RAW_TRANSHEADERS:
			putMapMetadataAsMap(MetadataType.CUSTOM, FRAGMENT_RAW_TRANSHEADERS, TransitiveType.NONE, false, context);
			break;
		case FRAGMENT_RAW_TRANSHEADERS_KV:
			putMapMetadataAsMap(MetadataType.CUSTOM, FRAGMENT_RAW_TRANSHEADERS_KV, TransitiveType.NONE, false, context);
			break;
		default:
			putMapMetadataAsMap(MetadataType.CUSTOM, fragment, TransitiveType.NONE, false, context);
			break;
		}
	}

	public static void setLocalService(String service) {
		LOCAL_SERVICE = service;
	}
}
