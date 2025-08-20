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

package com.tencent.cloud.plugin.unit.discovery;

import com.tencent.cloud.polaris.eager.instrument.feign.FeignEagerLoadSmartLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitFeignEagerLoadSmartLifecycle extends FeignEagerLoadSmartLifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(UnitFeignEagerLoadSmartLifecycle.class);


	public UnitFeignEagerLoadSmartLifecycle() {
		super(null, null, null);
	}

	@Override
	public void start() {
		LOG.info("ignore feign eager load in unit mode");
	}
}
