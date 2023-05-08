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

package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.cloud.common.metadata.MetadataContextHolder;

/**
 * PolarisLoadBalancerRingHashKeyProvider.
 *
 * @author seanthefish
 */
public final class PolarisLoadBalancerRingHashKeyProvider {

	private static final String LOAD_BALANCER_HASH_KEY = "LOAD_BALANCER_HASH_KEY";

	private PolarisLoadBalancerRingHashKeyProvider() {
	}

	public static void hashKey(String key) {
		MetadataContextHolder.get().setLoadbalancer(LOAD_BALANCER_HASH_KEY, key);
	}

	static String getHashKey() {
		return (String) MetadataContextHolder.get().getLoadbalancerMetadata().get(LOAD_BALANCER_HASH_KEY);
	}

}
