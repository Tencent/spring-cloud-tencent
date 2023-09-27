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

	private static final ThreadLocal<MetadataContext> METADATA_CONTEXT = new InheritableThreadLocal<>();

	private static MetadataLocalProperties metadataLocalProperties;

	private static StaticMetadataManager staticMetadataManager;

	private MetadataContextHolder() {
	}

	/**
	 * Get metadata context. Create if not existing.
	 * @return METADATA_CONTEXT
	 */
	public static MetadataContext get() {
		if (METADATA_CONTEXT.get() != null) {
			return METADATA_CONTEXT.get();
		}

		if (metadataLocalProperties == null) {
			metadataLocalProperties = ApplicationContextAwareUtils.getApplicationContext().getBean(MetadataLocalProperties.class);
		}
		if (staticMetadataManager == null) {
			staticMetadataManager = ApplicationContextAwareUtils.getApplicationContext().getBean(StaticMetadataManager.class);
		}

		// init static transitive metadata
		MetadataContext metadataContext = new MetadataContext();
		metadataContext.setTransitiveMetadata(staticMetadataManager.getMergedStaticTransitiveMetadata());
		metadataContext.setDisposableMetadata(staticMetadataManager.getMergedStaticDisposableMetadata());

		if (StringUtils.hasText(staticMetadataManager.getTransHeader())) {
			metadataContext.setTransHeaders(staticMetadataManager.getTransHeader(), "");
		}

		METADATA_CONTEXT.set(metadataContext);

		return METADATA_CONTEXT.get();
	}

	/**
	 * Get disposable metadata value from thread local .
	 * @param key metadata key .
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
	 * @param metadataContext metadata context
	 */
	public static void set(MetadataContext metadataContext) {
		METADATA_CONTEXT.set(metadataContext);
	}

	/**
	 * Save metadata map to thread local.
	 * @param dynamicTransitiveMetadata custom metadata collection
	 * @param dynamicDisposableMetadata custom disposable metadata connection
	 */
	public static void init(Map<String, String> dynamicTransitiveMetadata, Map<String, String> dynamicDisposableMetadata) {
		// Init ThreadLocal.
		MetadataContextHolder.remove();
		MetadataContext metadataContext = MetadataContextHolder.get();

		// Save transitive metadata to ThreadLocal.
		if (!CollectionUtils.isEmpty(dynamicTransitiveMetadata)) {
			Map<String, String> staticTransitiveMetadata = metadataContext.getTransitiveMetadata();
			Map<String, String> mergedTransitiveMetadata = new HashMap<>();
			mergedTransitiveMetadata.putAll(staticTransitiveMetadata);
			mergedTransitiveMetadata.putAll(dynamicTransitiveMetadata);
			metadataContext.setTransitiveMetadata(Collections.unmodifiableMap(mergedTransitiveMetadata));
		}
		if (!CollectionUtils.isEmpty(dynamicDisposableMetadata)) {
			Map<String, String> mergedUpstreamDisposableMetadata = new HashMap<>(dynamicDisposableMetadata);
			metadataContext.setUpstreamDisposableMetadata(Collections.unmodifiableMap(mergedUpstreamDisposableMetadata));
		}
		Map<String, String> staticDisposableMetadata = metadataContext.getDisposableMetadata();
		metadataContext.setDisposableMetadata(Collections.unmodifiableMap(staticDisposableMetadata));
		MetadataContextHolder.set(metadataContext);
	}

	/**
	 * Remove metadata context.
	 */
	public static void remove() {
		METADATA_CONTEXT.remove();
	}
}
