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

package com.tencent.cloud.metadata.util;

import java.util.HashMap;
import java.util.Map;


import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE;

/**
 * DefaultTransferMedataUtils.
 *
 * @author seanyu 2023-02-27
 */
public final class DefaultTransferMedataUtils {

	private DefaultTransferMedataUtils() {
	}

	public static Map<String, String> getDefaultTransferMedata() {
		return new HashMap<String, String>() {{
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, LOCAL_NAMESPACE);
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAME, LOCAL_SERVICE);
		}};
	}
}
