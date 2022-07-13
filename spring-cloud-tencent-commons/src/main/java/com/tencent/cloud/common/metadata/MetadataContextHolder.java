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
import java.util.Iterator;
import java.util.Map;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.INTERNAL_METADATA_DISPOSABLE;
import static com.tencent.cloud.common.util.JacksonUtils.deserialize2Map;
import static com.tencent.cloud.common.util.JacksonUtils.serialize2Json;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

/**
 * Metadata Context Holder.
 *
 * @author Haotian Zhang
 */
public final class MetadataContextHolder {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataContextHolder.class);

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
			metadataLocalProperties = (MetadataLocalProperties) ApplicationContextAwareUtils
					.getApplicationContext().getBean("metadataLocalProperties");
		}
		if (staticMetadataManager == null) {
			staticMetadataManager = (StaticMetadataManager) ApplicationContextAwareUtils
					.getApplicationContext().getBean("metadataManager");
		}

		// init static transitive metadata
		MetadataContext metadataContext = new MetadataContext();
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE,
				staticMetadataManager.getMergedStaticTransitiveMetadata());

		METADATA_CONTEXT.set(metadataContext);

		return METADATA_CONTEXT.get();
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
	 */
	public static void init(Map<String, String> dynamicTransitiveMetadata) {
		// Init ThreadLocal.
		MetadataContextHolder.remove();
		MetadataContext metadataContext = MetadataContextHolder.get();

		// Save transitive metadata to ThreadLocal.
		if (!CollectionUtils.isEmpty(dynamicTransitiveMetadata)) {

			// processing disposable keys
			if (dynamicTransitiveMetadata.containsKey(INTERNAL_METADATA_DISPOSABLE)) {
				String disposableKeyStatus = dynamicTransitiveMetadata.get(INTERNAL_METADATA_DISPOSABLE);
				try {
					if (StringUtils.hasText(disposableKeyStatus)) {
						Map<String, String> keyStatus = deserialize2Map(decode(disposableKeyStatus, UTF_8));
						Iterator<Map.Entry<String, String>> it = keyStatus.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<String, String> entry = it.next();
							String key = entry.getKey();
							boolean status = Boolean.parseBoolean(entry.getValue());
							if (!status) {
								keyStatus.put(key, "true");
							}
							else {
								// removed disposable key
								dynamicTransitiveMetadata.remove(key);
								it.remove();
							}
						}
						// reset
						dynamicTransitiveMetadata.put(INTERNAL_METADATA_DISPOSABLE, encode(serialize2Json(keyStatus), UTF_8));
					}
				}
				catch (Exception e) {
					LOGGER.error("Runtime system does not support utf-8 coding.", e);
				}

			}

			Map<String, String> staticTransitiveMetadata =
					metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
			Map<String, String> mergedTransitiveMetadata = new HashMap<>();
			mergedTransitiveMetadata.putAll(staticTransitiveMetadata);
			mergedTransitiveMetadata.putAll(dynamicTransitiveMetadata);

			metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE,
					Collections.unmodifiableMap(mergedTransitiveMetadata));
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
