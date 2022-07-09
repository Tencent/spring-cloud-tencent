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

package com.tencent.cloud.metadata.concurrent;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.assertj.core.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wlx
 * @date 2022/7/9 3:23 下午
 */
public class MetadataTestUtil {

	public static void initMetadataContext() {
		Map<String, String> customMetadata = new HashMap<>();
		customMetadata.put("a", "1");
		customMetadata.put("b", "2");

		MetadataContext metadataContext = MetadataContextHolder.get();

		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE, customMetadata);
		MetadataContextHolder.set(metadataContext);

		customMetadata = MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		Assertions.assertThat(customMetadata.get("a")).isEqualTo("1");
		Assertions.assertThat(customMetadata.get("b")).isEqualTo("2");

	}
}
