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

package com.tencent.cloud.plugin.pushgateway;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.stat.pushgateway.handler.PrometheusPushHandlerConfig;

import org.springframework.core.env.Environment;

/**
 * @author lingxiao.wlx
 */
public class PolarisStatPushGatewayModifier implements PolarisConfigModifier {

	private static final String POLARIS_STAT_ENABLED = "spring.cloud.polaris.stat.enabled";

	private static final String PROMETHEUS_PUSH_GATEWAY_PLUGIN_NAME = "prometheus-pushgateway";

	private final PolarisStatPushGatewayProperties polarisStatPushGatewayProperties;

	private final Environment environment;

	public PolarisStatPushGatewayModifier(PolarisStatPushGatewayProperties polarisStatPushGatewayProperties,
				Environment environment) {
		this.polarisStatPushGatewayProperties = polarisStatPushGatewayProperties;
		this.environment = environment;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// Turn on stat reporter configuration.
		Boolean statEnabled = environment.getProperty(POLARIS_STAT_ENABLED, Boolean.class, false);
		if (!statEnabled) {
			configuration.getGlobal().getStatReporter().setEnable(polarisStatPushGatewayProperties.isEnabled());
		}

		// Set prometheus plugin.
		if (polarisStatPushGatewayProperties.isEnabled()) {
			PrometheusPushHandlerConfig prometheusHandlerConfig = configuration.getGlobal().getStatReporter()
					.getPluginConfig(PROMETHEUS_PUSH_GATEWAY_PLUGIN_NAME, PrometheusPushHandlerConfig.class);
			prometheusHandlerConfig.setPushgatewayAddress(polarisStatPushGatewayProperties.getAddress());
			prometheusHandlerConfig.setPushgatewayNamespace(polarisStatPushGatewayProperties.getNamespace());
			prometheusHandlerConfig.setPushgatewayService(polarisStatPushGatewayProperties.getService());
			prometheusHandlerConfig.setPushInterval(polarisStatPushGatewayProperties.getPushInterval());
			configuration.getGlobal().getStatReporter()
					.setPluginConfig(PROMETHEUS_PUSH_GATEWAY_PLUGIN_NAME, prometheusHandlerConfig);
		}
	}

	@Override
	public int getOrder() {
		// run after prometheus stat reporter.
		return ContextConstant.ModifierOrder.STAT_REPORTER_ORDER + 1;
	}
}
