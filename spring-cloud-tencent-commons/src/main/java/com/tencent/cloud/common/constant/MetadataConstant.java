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

package com.tencent.cloud.common.constant;

import org.springframework.core.Ordered;

/**
 * Constant for metadata.
 *
 * @author Haotian Zhang
 */
public final class MetadataConstant {

	/**
	 * Order of filter, interceptor, ...
	 */
	public static class OrderConstant {

		/**
		 * Order of filter.
		 */
		public static final int WEB_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 13;

		/**
		 * Order of MetadataFirstFeignInterceptor.
		 */
		public static int METADATA_FIRST_FEIGN_INTERCEPTOR_ORDER = Ordered.HIGHEST_PRECEDENCE
				+ 1;

		/**
		 * Order of Metadata2HeaderInterceptor.
		 */
		public static int METADATA_2_HEADER_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE;

	}

	/**
	 * Metadata HTTP header name.
	 */
	public static class HeaderName {

		/**
		 * Custom metadata.
		 */
		public static final String CUSTOM_METADATA = "SCT-CUSTOM-METADATA";

		/**
		 * Peer service name.
		 */
		public static final String PEER_SERVICE = "SCT-PEER-SERVICE";

		/**
		 * Metadata context.
		 */
		public static final String METADATA_CONTEXT = "SCT-METADATA-CONTEXT";

	}

}
