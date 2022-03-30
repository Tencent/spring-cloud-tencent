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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;

/**
 * Metadata Context.
 *
 * @author Haotian Zhang
 */
public class MetadataContext {

	/**
	 * Namespace of local instance.
	 */
	public static final String LOCAL_NAMESPACE = ApplicationContextAwareUtils
			.getProperties("spring.cloud.polaris.discovery.namespace", "default");

	/**
	 * Service name of local instance.
	 */
	public static final String LOCAL_SERVICE = ApplicationContextAwareUtils.getProperties(
			"spring.cloud.polaris.discovery.service",
			ApplicationContextAwareUtils.getProperties("spring.application.name", null));

	/**
	 * Transitive custom metadata content.
	 */
	private final Map<String, String> transitiveCustomMetadata;

	/**
	 * System metadata content.
	 */
	private final Map<String, String> systemMetadata;

	public MetadataContext() {
		this.transitiveCustomMetadata = new ConcurrentHashMap<>();
		this.systemMetadata = new ConcurrentHashMap<>();
	}

	public Map<String, String> getAllTransitiveCustomMetadata() {
		return Collections.unmodifiableMap(this.transitiveCustomMetadata);
	}

	public String getTransitiveCustomMetadata(String key) {
		return this.transitiveCustomMetadata.get(key);
	}

	public void putTransitiveCustomMetadata(String key, String value) {
		this.transitiveCustomMetadata.put(key, value);
	}

	public void putAllTransitiveCustomMetadata(Map<String, String> customMetadata) {
		this.transitiveCustomMetadata.putAll(customMetadata);
	}

	public Map<String, String> getAllSystemMetadata() {
		return Collections.unmodifiableMap(this.systemMetadata);
	}

	public String getSystemMetadata(String key) {
		return this.systemMetadata.get(key);
	}

	public void putSystemMetadata(String key, String value) {
		this.systemMetadata.put(key, value);
	}

	public void putAllSystemMetadata(Map<String, String> systemMetadata) {
		this.systemMetadata.putAll(systemMetadata);
	}

	@Override
	public String toString() {
		return "MetadataContext{" + "transitiveCustomMetadata="
				+ JacksonUtils.serialize2Json(transitiveCustomMetadata)
				+ ", systemMetadata=" + JacksonUtils.serialize2Json(systemMetadata) + '}';
	}

}
