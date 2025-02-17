/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.context.event;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.event.pushgateway.PushGatewayEventReporterConfig;

/**
 * Modifier for push gateway event reporter.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventReporterConfigModifier implements PolarisConfigModifier {

	private final PushGatewayEventReporterProperties properties;

	public PushGatewayEventReporterConfigModifier(PushGatewayEventReporterProperties properties) {
		this.properties = properties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		configuration.getGlobal().getEventReporter().getReporters()
				.add(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE);

		PushGatewayEventReporterConfig pushGatewayEventReporterConfig = new PushGatewayEventReporterConfig();
		if (!properties.isEnabled() || StringUtils.isBlank(properties.getAddress())) {
			pushGatewayEventReporterConfig.setEnable(false);
			return;
		}
		else {
			pushGatewayEventReporterConfig.setEnable(true);
		}
		pushGatewayEventReporterConfig.setAddress(properties.getAddress());
		pushGatewayEventReporterConfig.setEventQueueSize(properties.getEventQueueSize());
		pushGatewayEventReporterConfig.setMaxBatchSize(properties.getMaxBatchSize());

		configuration.getGlobal().getEventReporter()
				.setPluginConfig(DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE, pushGatewayEventReporterConfig);
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.PUSH_GATEWAY_EVENT_ORDER;
	}
}
