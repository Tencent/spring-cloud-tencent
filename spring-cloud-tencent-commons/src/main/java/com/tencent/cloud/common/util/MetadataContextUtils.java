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

package com.tencent.cloud.common.util;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;

public final class MetadataContextUtils {

	private MetadataContextUtils() {

	}

	public static String getCallerApplicationMetadataStringValue(MetadataContext metadataContext, String key, String defaultValue)  {
		MetadataObjectValue<String> metadataObjectValue = metadataContext.getMetadataContainer(MetadataType.APPLICATION, true).getMetadataValue(key);
		if (metadataObjectValue == null) {
			return defaultValue;
		}
		return metadataObjectValue.getObjectValue().orElse(defaultValue);
	}

	public static void putCallerApplicationMetadataStringValue(MetadataContext metadataContext, String key, String value) {
		metadataContext.getMetadataContainer(MetadataType.APPLICATION, true).putMetadataObjectValue(key, value);
	}
}
