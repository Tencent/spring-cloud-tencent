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

/**
 * Constant for metadata.
 *
 * @author Haotian Zhang
 */
public final class MetadataConstant {

	/**
	 * sct transitive header prefix.
	 */
	public static final String SCT_TRANSITIVE_HEADER_PREFIX = "X-SCT-Metadata-Transitive-";
	/**
	 * sct transitive header prefix length.
	 */
	public static final int SCT_TRANSITIVE_HEADER_PREFIX_LENGTH = SCT_TRANSITIVE_HEADER_PREFIX.length();

	/**
	 * polaris transitive header prefix.
	 */
	public static final String POLARIS_TRANSITIVE_HEADER_PREFIX = "X-Polaris-Metadata-Transitive-";
	/**
	 * polaris transitive header prefix length.
	 */
	public static final int POLARIS_TRANSITIVE_HEADER_PREFIX_LENGTH = POLARIS_TRANSITIVE_HEADER_PREFIX.length();

	private MetadataConstant() {

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
		 * Custom Disposable Metadata.
		 */
		public static final String CUSTOM_DISPOSABLE_METADATA = "SCT-CUSTOM-DISPOSABLE-METADATA";

		/**
		 * System Metadata.
		 */
		public static final String SYSTEM_METADATA = "SCT-SYSTEM-METADATA";

		/**
		 * Metadata context.
		 */
		public static final String METADATA_CONTEXT = "SCT-METADATA-CONTEXT";
	}

	public static class DefaultMetadata {

		/**
		 * Default Metadata Source Service Namespace Key.
		 */
		public static final String DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE = "source_service_namespace";

		/**
		 * Default Metadata Source Service Name Key.
		 */
		public static final String DEFAULT_METADATA_SOURCE_SERVICE_NAME = "source_service_name";

	}

}
