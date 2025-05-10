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

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tencent.cloud.common.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metadata Context for cross thread.
 *
 * @author Haotian Zhang
 */
public class CrossThreadMetadataContext {

	private static final Logger LOG = LoggerFactory.getLogger(CrossThreadMetadataContext.class);

	/**
	 * Get metadata context from previous thread.
	 */
	public static final Supplier<CrossThreadMetadataContext> CROSS_THREAD_METADATA_CONTEXT_SUPPLIER = () -> {
		CrossThreadMetadataContext crossThreadMetadataContext = new CrossThreadMetadataContext();
		crossThreadMetadataContext.setMetadataContext(MetadataContextHolder.get());

		if (LOG.isDebugEnabled()) {
			LOG.debug("Context map is got: {}", JacksonUtils.serialize2Json(crossThreadMetadataContext));
		}
		return crossThreadMetadataContext;
	};

	/**
	 * Set metadata context to current thread.
	 */
	public static final Consumer<CrossThreadMetadataContext> CROSS_THREAD_METADATA_CONTEXT_CONSUMER = crossThreadMetadataContext -> {
		MetadataContextHolder.set(crossThreadMetadataContext.getMetadataContext());
		if (LOG.isDebugEnabled()) {
			LOG.debug("Context map is set: {}", JacksonUtils.serialize2Json(crossThreadMetadataContext));
		}
	};

	private MetadataContext metadataContext;

	public MetadataContext getMetadataContext() {
		return metadataContext;
	}

	public void setMetadataContext(MetadataContext metadataContext) {
		this.metadataContext = metadataContext;
	}
}
