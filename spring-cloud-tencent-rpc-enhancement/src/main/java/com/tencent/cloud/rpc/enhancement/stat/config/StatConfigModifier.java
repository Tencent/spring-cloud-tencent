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

package com.tencent.cloud.rpc.enhancement.stat.config;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;

import static com.tencent.polaris.api.config.global.StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;

/**
 * Config modifier for stat reporter.
 *
 * @author Haotian Zhang
 */
public class StatConfigModifier implements PolarisConfigModifier {

	private final PolarisStatProperties polarisStatProperties;

	public StatConfigModifier(PolarisStatProperties polarisStatProperties) {
		this.polarisStatProperties = polarisStatProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		// Turn on stat reporter configuration.
		configuration.getGlobal().getStatReporter().setEnable(polarisStatProperties.isEnabled());
		StatReporterConfigImpl statReporterConfig = configuration.getGlobal().getStatReporter();
		statReporterConfig.setEnable(polarisStatProperties.isEnabled());
		PrometheusHandlerConfig prometheusHandlerConfig = statReporterConfig.getPluginConfig(DEFAULT_REPORTER_PROMETHEUS, PrometheusHandlerConfig.class);
		prometheusHandlerConfig.setPathRegexList(polarisStatProperties.getPathRegexList());
		// Set prometheus plugin.
		if (polarisStatProperties.isEnabled()) {
			if (polarisStatProperties.isPushGatewayEnabled()) {
				// push gateway
				prometheusHandlerConfig.setType("push");
				prometheusHandlerConfig.setAddress(polarisStatProperties.getPushGatewayAddress());
				prometheusHandlerConfig.setNamespace(polarisStatProperties.getStatNamespace());
				prometheusHandlerConfig.setService(polarisStatProperties.getStatService());
				prometheusHandlerConfig.setPushInterval(polarisStatProperties.getPushGatewayPushInterval());
				prometheusHandlerConfig.setOpenGzip(polarisStatProperties.getOpenGzip());
			}
			else {
				// pull metrics
				prometheusHandlerConfig.setType("pull");
				prometheusHandlerConfig.setPath(polarisStatProperties.getPath());
			}
		}
		configuration.getGlobal().getStatReporter()
				.setPluginConfig(DEFAULT_REPORTER_PROMETHEUS, prometheusHandlerConfig);
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.STAT_REPORTER_ORDER;
	}
}
