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

package com.tencent.cloud.polaris.context.config.extend.tsf;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.extend.consul.ConsulProperties;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.event.tsf.TsfEventReporterConfig;

/**
 * Config modifier for TSF.
 *
 * @author Haotian Zhang
 */
public class TsfContextConfigModifier implements PolarisConfigModifier {

	private final TsfCoreProperties tsfCoreProperties;

	private final ConsulProperties consulProperties;

	public TsfContextConfigModifier(TsfCoreProperties tsfCoreProperties, ConsulProperties consulProperties) {
		this.tsfCoreProperties = tsfCoreProperties;
		this.consulProperties = consulProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		configuration.getGlobal().getEventReporter().getReporters().add(DefaultPlugins.TSF_EVENT_REPORTER_TYPE);

		TsfEventReporterConfig tsfEventReporterConfig = new TsfEventReporterConfig();
		if (StringUtils.isNotBlank(tsfCoreProperties.getEventMasterIp())) {
			tsfEventReporterConfig.setEnable(true);
		}
		else {
			tsfEventReporterConfig.setEnable(false);
			return;
		}
		tsfEventReporterConfig.setEventMasterIp(tsfCoreProperties.getEventMasterIp());
		tsfEventReporterConfig.setEventMasterPort(tsfCoreProperties.getEventMasterPort());
		tsfEventReporterConfig.setAppId(tsfCoreProperties.getAppId());
		tsfEventReporterConfig.setRegion(tsfCoreProperties.getTsfRegion());
		tsfEventReporterConfig.setInstanceId(tsfCoreProperties.getInstanceId());
		tsfEventReporterConfig.setTsfNamespaceId(tsfCoreProperties.getTsfNamespaceId());
		tsfEventReporterConfig.setServiceName(tsfCoreProperties.getServiceName());
		tsfEventReporterConfig.setToken(consulProperties.getAclToken());
		tsfEventReporterConfig.setApplicationId(tsfCoreProperties.getTsfApplicationId());
		configuration.getGlobal().getEventReporter()
				.setPluginConfig(DefaultPlugins.TSF_EVENT_REPORTER_TYPE, tsfEventReporterConfig);
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.CIRCUIT_BREAKER_ORDER - 1;
	}
}
