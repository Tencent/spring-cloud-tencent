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

package com.tencent.cloud.polaris.config.adapter;

import java.util.Set;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;

import org.springframework.cloud.context.refresh.ContextRefresher;

/**
 * The default implement of Spring Cloud refreshes the entire Spring Context.
 * The disadvantage is that the entire context is rebuilt, which has a large impact and low performance.
 *
 * @author lingxiao.wlx
 */
public class PolarisRefreshEntireContextRefresher extends PolarisConfigPropertyAutoRefresher {

	private final ContextRefresher contextRefresher;

	public PolarisRefreshEntireContextRefresher(PolarisConfigProperties polarisConfigProperties,
			PolarisPropertySourceManager polarisPropertySourceManager,
			ContextRefresher contextRefresher) {
		super(polarisConfigProperties, polarisPropertySourceManager);
		this.contextRefresher = contextRefresher;
	}

	@Override
	public void refreshConfigurationProperties(Set<String> changeKeys) {
		contextRefresher.refresh();
	}
}
