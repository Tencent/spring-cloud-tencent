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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;

import org.springframework.util.CollectionUtils;

/**
 * Metadata Context Holder.
 *
 * @author Haotian Zhang
 */
public final class MetadataContextHolder {

	private static final ThreadLocal<MetadataContext> METADATA_CONTEXT = new InheritableThreadLocal<>();

	private static MetadataLocalProperties metadataLocalProperties;

	private MetadataContextHolder() {

	}

	/**
	 * Get metadata context. Create if not existing.
	 * @return METADATA_CONTEXT
	 */
	public static MetadataContext get() {
		if (null == METADATA_CONTEXT.get()) {
			MetadataContext metadataContext = new MetadataContext();
			if (metadataLocalProperties == null) {
				metadataLocalProperties = (MetadataLocalProperties) ApplicationContextAwareUtils
						.getApplicationContext().getBean("metadataLocalProperties");
			}

			// init custom metadata and load local metadata
			Map<String, String> transitiveMetadataMap = getTransitiveMetadataMap(
					metadataLocalProperties.getContent(),
					metadataLocalProperties.getTransitive());
			metadataContext.putAllTransitiveCustomMetadata(transitiveMetadataMap);

			METADATA_CONTEXT.set(metadataContext);
		}
		return METADATA_CONTEXT.get();
	}

	/**
	 * Filter and store the transitive metadata to transitive metadata context.
	 * @param source all metadata content
	 * @param transitiveMetadataKeyList transitive metadata name list
	 * @return result
	 */
	private static Map<String, String> getTransitiveMetadataMap(
			Map<String, String> source, List<String> transitiveMetadataKeyList) {
		Map<String, String> result = new HashMap<>();
		for (String key : transitiveMetadataKeyList) {
			if (source.containsKey(key)) {
				result.put(key, source.get(key));
			}
		}
		return result;
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
	 * @param customMetadataMap custom metadata collection
	 * @param systemMetadataMap system metadata collection
	 */
	public static void init(Map<String, String> customMetadataMap,
			Map<String, String> systemMetadataMap) {
		// Init ThreadLocal.
		MetadataContextHolder.remove();
		MetadataContext metadataContext = MetadataContextHolder.get();

		// Save to ThreadLocal.
		if (!CollectionUtils.isEmpty(customMetadataMap)) {
			metadataContext.putAllTransitiveCustomMetadata(customMetadataMap);
		}
		if (!CollectionUtils.isEmpty(systemMetadataMap)) {
			metadataContext.putAllSystemMetadata(systemMetadataMap);
		}
		MetadataContextHolder.set(metadataContext);
	}

	/**
	 * Remove metadata context.
	 */
	public static void remove() {
		METADATA_CONTEXT.remove();
	}

}
