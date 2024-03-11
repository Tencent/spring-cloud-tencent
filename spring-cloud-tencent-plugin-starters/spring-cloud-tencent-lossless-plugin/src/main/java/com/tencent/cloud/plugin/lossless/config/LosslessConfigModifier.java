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

package com.tencent.cloud.plugin.lossless.config;

import java.util.Objects;

import com.tencent.cloud.common.constant.OrderConstant.Modifier;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.provider.LosslessConfigImpl;

/**
 * Config modifier for lossless.
 *
 * @author Shedfree Wu
 */
public class LosslessConfigModifier implements PolarisConfigModifier {

	private final LosslessProperties losslessProperties;

	public LosslessConfigModifier(LosslessProperties losslessProperties) {
		this.losslessProperties = losslessProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (losslessProperties.isEnabled()) {
			LosslessConfigImpl losslessConfig = (LosslessConfigImpl) configuration.getProvider().getLossless();
			losslessConfig.setEnable(true);
			losslessConfig.setPort(losslessProperties.getPort());
			if (Objects.nonNull(losslessProperties.getDelayRegisterInterval())) {
				losslessConfig.setDelayRegisterInterval(losslessProperties.getDelayRegisterInterval());
			}
			if (Objects.nonNull(losslessProperties.getHealthCheckInterval())) {
				losslessConfig.setHealthCheckInterval(losslessProperties.getHealthCheckInterval());
			}
		}
	}

	@Override
	public int getOrder() {
		return Modifier.LOSSLESS_ORDER;
	}
}
