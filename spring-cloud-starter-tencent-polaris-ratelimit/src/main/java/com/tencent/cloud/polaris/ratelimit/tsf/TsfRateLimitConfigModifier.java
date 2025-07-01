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

package com.tencent.cloud.polaris.ratelimit.tsf;

import java.util.Map;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.cloud.polaris.context.config.extend.consul.ConsulProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfContextUtils;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.ratelimit.client.sync.tsf.TsfRateLimitConstants;

import org.springframework.core.env.Environment;

/**
 * Config modifier for TSF rate limit.
 *
 * @author Haotian Zhang
 */
public class TsfRateLimitConfigModifier implements PolarisConfigModifier {

	private final TsfCoreProperties tsfCoreProperties;

	private final ConsulProperties consulProperties;

	private final Environment environment;

	public TsfRateLimitConfigModifier(TsfCoreProperties tsfCoreProperties, ConsulProperties consulProperties,
			Environment environment) {
		this.tsfCoreProperties = tsfCoreProperties;
		this.consulProperties = consulProperties;
		this.environment = environment;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		if (TsfContextUtils.isTsfConsulEnabled(environment)) {
			Map<String, String> metadata = configuration.getProvider().getRateLimit().getMetadata();
			metadata.put(TsfRateLimitConstants.RATE_LIMIT_MASTER_IP_KEY, tsfCoreProperties.getRatelimitMasterIp());
			metadata.put(TsfRateLimitConstants.RATE_LIMIT_MASTER_PORT_KEY, String.valueOf(tsfCoreProperties.getRatelimitMasterPort()));
			metadata.put(TsfRateLimitConstants.SERVICE_NAME_KEY, tsfCoreProperties.getServiceName());
			metadata.put(TsfRateLimitConstants.INSTANCE_ID_KEY, tsfCoreProperties.getInstanceId());
			metadata.put(TsfRateLimitConstants.TOKEN_KEY, consulProperties.getAclToken());
		}
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.RATE_LIMIT_ORDER;
	}
}
