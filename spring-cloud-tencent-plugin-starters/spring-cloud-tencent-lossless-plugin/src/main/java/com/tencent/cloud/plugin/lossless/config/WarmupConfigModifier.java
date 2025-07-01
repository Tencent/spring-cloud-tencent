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

package com.tencent.cloud.plugin.lossless.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.tencent.cloud.common.constant.OrderConstant.Modifier;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.WeightAdjustConfigImpl;
import com.tencent.polaris.plugin.lossless.warmup.WarmupWeightAdjuster;

/**
 * Config modifier for warmup.
 *
 * @author Shedfree Wu
 */
public class WarmupConfigModifier implements PolarisConfigModifier {

	private final WarmupProperties warmupProperties;

	public WarmupConfigModifier(WarmupProperties warmupProperties) {
		this.warmupProperties = warmupProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		WeightAdjustConfigImpl weightAdjustConfig = (WeightAdjustConfigImpl) configuration.getConsumer().getWeightAdjust();
		if (warmupProperties.isEnabled()) {
			Set<String> chainSet = new TreeSet<>(
					Optional.ofNullable(weightAdjustConfig.getChain()).orElse(Collections.emptyList()));
			chainSet.add(WarmupWeightAdjuster.WARMUP_WEIGHT_ADJUSTER_NAME);
			weightAdjustConfig.setChain(new ArrayList<>(chainSet));
		}
		else {
			Set<String> chainSet = new TreeSet<>(
					Optional.ofNullable(weightAdjustConfig.getChain()).orElse(Collections.emptyList()));
			chainSet.remove(WarmupWeightAdjuster.WARMUP_WEIGHT_ADJUSTER_NAME);
			weightAdjustConfig.setChain(new ArrayList<>(chainSet));
		}
	}

	@Override
	public int getOrder() {
		return Modifier.LOSSLESS_ORDER;
	}
}
