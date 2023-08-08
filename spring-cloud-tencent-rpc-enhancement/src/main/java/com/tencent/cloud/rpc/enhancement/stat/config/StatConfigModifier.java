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

package com.tencent.cloud.rpc.enhancement.stat.config;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import static com.tencent.polaris.api.config.global.StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;

/**
 * Config modifier for stat reporter.
 *
 * @author Haotian Zhang
 */
public class StatConfigModifier implements PolarisConfigModifier {

	private final PolarisStatProperties polarisStatProperties;

	private final Environment environment;

	public StatConfigModifier(PolarisStatProperties polarisStatProperties, Environment environment) {
		this.polarisStatProperties = polarisStatProperties;
		this.environment = environment;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// Turn on stat reporter configuration.
		configuration.getGlobal().getStatReporter().setEnable(polarisStatProperties.isEnabled());
		PrometheusHandlerConfig prometheusHandlerConfig = configuration.getGlobal().getStatReporter()
				.getPluginConfig(DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
		// Set prometheus plugin.
		if (polarisStatProperties.isEnabled()) {

			if (polarisStatProperties.isPushGatewayEnabled()) {
				// push gateway
				prometheusHandlerConfig.setType("push");
				prometheusHandlerConfig.setAddress(polarisStatProperties.getPushGatewayAddress());
				prometheusHandlerConfig.setPushInterval(polarisStatProperties.getPushGatewayPushInterval());
			}
			else {
				// pull metrics
				prometheusHandlerConfig.setType("pull");
				if (!StringUtils.hasText(polarisStatProperties.getHost())) {
					polarisStatProperties.setHost(environment.getProperty("spring.cloud.client.ip-address"));
				}
				prometheusHandlerConfig.setHost(polarisStatProperties.getHost());
				prometheusHandlerConfig.setPort(polarisStatProperties.getPort());
				prometheusHandlerConfig.setPath(polarisStatProperties.getPath());
			}

		}
		else {
			// Set port to -1 to disable stat plugin.
			prometheusHandlerConfig.setPort(-1);
		}
		configuration.getGlobal().getStatReporter()
				.setPluginConfig(DEFAULT_REPORTER_PROMETHEUS, prometheusHandlerConfig);
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.STAT_REPORTER_ORDER;
	}
}
