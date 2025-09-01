/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.common.util;

import java.util.Optional;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for MetadataContext.
 *
 * @author Shedfree Wu
 */
public final class MetadataContextUtils {

	private static final Logger LOG = LoggerFactory.getLogger(MetadataContextUtils.class);

	private MetadataContextUtils() {
	}

	/**
	 * use callee's custom metadata to store local thread's metadata. caller's metadata is for remote upstream service.
	 */
	public static void putMetadataObjectValue(String key, Object value) {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.CUSTOM, false).
				putMetadataObjectValue(key, value);
	}

	public static boolean existMetadataValue(MetadataObjectValue<?> metadataObjectValue) {
		return Optional.ofNullable(metadataObjectValue).map(MetadataObjectValue::getObjectValue).
				map(Optional::isPresent).orElse(false);
	}
}
