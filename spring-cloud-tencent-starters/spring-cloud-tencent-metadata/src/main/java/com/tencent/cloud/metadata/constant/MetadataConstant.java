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

package com.tencent.cloud.metadata.constant;

import org.springframework.core.Ordered;

/**
 * Constant for spring-cloud-tencent-metadata.
 *
 * @author Haotian Zhang
 */
public interface MetadataConstant {

    /**
     * Metadata HTTP header name.
     */
    interface HeaderName {

        /**
         * Custom metadata
         */
        String CUSTOM_METADATA = "SCT-CUSTOM-METADATA";

        /**
         * System Metadata
         */
        String SYSTEM_METADATA = "SCT-SYSTEM-METADATA";

        /**
         * Metadata context
         */
        String METADATA_CONTEXT = "SCT-METADATA-CONTEXT";
    }

    /**
     * Order of filter, interceptor, ...
     */
    interface OrderConstant {

        /**
         * Order of filter
         */
        int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 3;

        /**
         * Order of MetadataFirstFeignPlugin
         */
        int METADATA_FIRST_FEIGN_PLUGIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

        /**
         * Order of Metadata2HeaderFeignInterceptor
         */
        int METADATA_2_HEADER_FEIGN_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE;

        /**
         * Order of interceptor
         */
        int INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * System metadata key
     */
    interface SystemMetadataKey {

        /**
         * Local namespace
         */
        String LOCAL_NAMESPACE = "LOCAL_NAMESPACE";

        /**
         * Local service
         */
        String LOCAL_SERVICE = "LOCAL_SERVICE";

        /**
         * Local path
         */
        String LOCAL_PATH = "LOCAL_PATH";

        /**
         * Peer namespace
         */
        String PEER_NAMESPACE = "PEER_NAMESPACE";

        /**
         * Peer service
         */
        String PEER_SERVICE = "PEER_SERVICE";

        /**
         * Peer path
         */
        String PEER_PATH = "PEER_PATH";
    }
}
