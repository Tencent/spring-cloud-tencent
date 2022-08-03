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

package com.tencent.cloud.common.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Instance's custom metadata, metadata will be register to polaris server.
 *
 * @author lepdou 2022-06-16
 */
public interface InstanceMetadataProvider {

	/**
	 * @return the metadata of instance.
	 */
	default Map<String, String> getMetadata() {
		return Collections.emptyMap();
	}

	/**
	 * @return the keys of transitive metadata.
	 */
	default Set<String> getTransitiveMetadataKeys() {
		return Collections.emptySet();
	}

	/**
	 * @return the keys of disposable metadata.
	 */
	default Set<String> getDisposableMetadataKeys() {
		return Collections.emptySet();
	}

	/**
	 * The region of current instance.
	 *
	 * @return the region info.
	 */
	default String getRegion() {
		return "";
	}

	/**
	 * The zone of current instance.
	 *
	 * @return the zone info.
	 */
	default String getZone() {
		return "";
	}

	/**
	 * The campus/datacenter of current instance.
	 *
	 * @return the campus or datacenter info.
	 */
	default String getCampus() {
		return "";
	}
}
