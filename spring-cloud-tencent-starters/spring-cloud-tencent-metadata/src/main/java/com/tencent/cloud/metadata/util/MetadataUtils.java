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

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Processing metadata.
 *
 * @author Haotian Zhang
 */
public class MetadataUtils {

    /**
     * merge metadata map and new metadata map string.
     *
     * @param localCustomMetadataMap
     * @param newMetadataStr
     * @return
     */
    public static Map<String, String> loadAndMergeCustomMetadata(Map<String, String> localCustomMetadataMap,
                                                                 String newMetadataStr) {
        // Load local metadata.
        Map<String, String> metadataMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(localCustomMetadataMap)) {
            metadataMap.putAll(localCustomMetadataMap);
        }

        // Transfer string to map.
        if (StringUtils.hasText(newMetadataStr)) {
            Map<String, String> requestMetadataMap = JacksonUtils.deserialize2Map(newMetadataStr);

            // metadata from upstream cover local metadata for this thread.
            for (String key : requestMetadataMap.keySet()) {
                metadataMap.put(key, requestMetadataMap.get(key));
            }
        }

        return metadataMap;
    }
}
