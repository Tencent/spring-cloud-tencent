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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.metadata.MetadataContext.FRAGMENT_DISPOSABLE;
import static com.tencent.cloud.common.metadata.MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE;

/**
 * Metadata Context Holder.
 *
 * @author Haotian Zhang
 */
public final class MetadataContextHolder {

	private static MetadataLocalProperties metadataLocalProperties;

	private static StaticMetadataManager staticMetadataManager;

	static {
		com.tencent.polaris.metadata.core.manager.MetadataContextHolder.setInitializer(MetadataContextHolder::createMetadataManager);
	}

	private MetadataContextHolder() {
	}

	public static MetadataContext get() {
		return (MetadataContext) com.tencent.polaris.metadata.core.manager.MetadataContextHolder.getOrCreate();
	}

	private static MetadataContext createMetadataManager() {
		MetadataContext metadataManager = new MetadataContext();
		if (metadataLocalProperties == null) {
			metadataLocalProperties = ApplicationContextAwareUtils.getApplicationContext()
					.getBean(MetadataLocalProperties.class);
		}
		if (staticMetadataManager == null) {
			staticMetadataManager = ApplicationContextAwareUtils.getApplicationContext()
					.getBean(StaticMetadataManager.class);
		}
		MetadataContainer metadataContainer = metadataManager.getMetadataContainer(MetadataType.CUSTOM, false);
		Map<String, String> mergedStaticTransitiveMetadata = staticMetadataManager.getMergedStaticTransitiveMetadata();
		for (Map.Entry<String, String> entry : mergedStaticTransitiveMetadata.entrySet()) {
			metadataContainer.putMetadataStringValue(entry.getKey(), entry.getValue(), TransitiveType.PASS_THROUGH);
		}
		Map<String, String> mergedStaticDisposableMetadata = staticMetadataManager.getMergedStaticDisposableMetadata();
		for (Map.Entry<String, String> entry : mergedStaticDisposableMetadata.entrySet()) {
			metadataContainer.putMetadataStringValue(entry.getKey(), entry.getValue(), TransitiveType.DISPOSABLE);
		}

		if (StringUtils.hasText(staticMetadataManager.getTransHeader())) {
			String transHeader = staticMetadataManager.getTransHeader();
			metadataContainer.putMetadataMapValue(MetadataContext.FRAGMENT_RAW_TRANSHEADERS, transHeader, "", TransitiveType.NONE);
		}
		return metadataManager;
	}

	/**
	 * Get disposable metadata value from thread local .
	 *
	 * @param key      metadata key .
	 * @param upstream upstream disposable , otherwise will return local static disposable metadata .
	 * @return target disposable metadata value .
	 */
	public static Optional<String> getDisposableMetadata(String key, boolean upstream) {
		MetadataContext context = get();
		if (upstream) {
			return Optional.ofNullable(context.getContext(FRAGMENT_UPSTREAM_DISPOSABLE, key));
		}
		else {
			return Optional.ofNullable(context.getContext(FRAGMENT_DISPOSABLE, key));
		}
	}

	/**
	 * Get all disposable metadata value from thread local .
	 *
	 * @param upstream upstream disposable , otherwise will return local static disposable metadata .
	 * @return target disposable metadata value .
	 */
	public static Map<String, String> getAllDisposableMetadata(boolean upstream) {
		Map<String, String> disposables = new HashMap<>();
		MetadataContext context = get();
		if (upstream) {
			disposables.putAll(context.getFragmentContext(FRAGMENT_UPSTREAM_DISPOSABLE));
		}
		else {
			disposables.putAll(context.getFragmentContext(FRAGMENT_DISPOSABLE));
		}
		return Collections.unmodifiableMap(disposables);
	}

	/**
	 * Set metadata context.
	 *
	 * @param metadataContext metadata context
	 */
	public static void set(MetadataContext metadataContext) {
		com.tencent.polaris.metadata.core.manager.MetadataContextHolder.set(metadataContext);
	}

	/**
	 * Save metadata map to thread local.
	 *
	 * @param dynamicTransitiveMetadata custom metadata collection
	 * @param dynamicDisposableMetadata custom disposable metadata connection
	 * @param callerMetadataProvider caller metadata provider
	 */
	public static void init(Map<String, String> dynamicTransitiveMetadata, Map<String, String> dynamicDisposableMetadata,
			MetadataProvider callerMetadataProvider) {
		com.tencent.polaris.metadata.core.manager.MetadataContextHolder.refresh(metadataManager -> {
			MetadataContainer metadataContainerUpstream = metadataManager.getMetadataContainer(MetadataType.CUSTOM, false);
			if (!CollectionUtils.isEmpty(dynamicTransitiveMetadata)) {
				for (Map.Entry<String, String> entry : dynamicTransitiveMetadata.entrySet()) {
					metadataContainerUpstream.putMetadataStringValue(entry.getKey(), entry.getValue(), TransitiveType.PASS_THROUGH);
				}
			}
			MetadataContainer metadataContainerDownstream = metadataManager.getMetadataContainer(MetadataType.CUSTOM, true);
			if (!CollectionUtils.isEmpty(dynamicDisposableMetadata)) {
				for (Map.Entry<String, String> entry : dynamicDisposableMetadata.entrySet()) {
					metadataContainerDownstream.putMetadataStringValue(entry.getKey(), entry.getValue(), TransitiveType.DISPOSABLE);
				}
			}
			if (callerMetadataProvider != null) {
				MessageMetadataContainer callerMessageContainer = metadataManager.getMetadataContainer(MetadataType.MESSAGE, true);
				callerMessageContainer.setMetadataProvider(callerMetadataProvider);
			}
		});
	}

	/**
	 * Remove metadata context.
	 */
	public static void remove() {
		com.tencent.polaris.metadata.core.manager.MetadataContextHolder.remove();
	}
}
