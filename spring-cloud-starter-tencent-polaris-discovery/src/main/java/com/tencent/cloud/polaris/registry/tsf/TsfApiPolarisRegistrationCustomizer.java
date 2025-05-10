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

package com.tencent.cloud.polaris.registry.tsf;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisRegistrationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;

/**
 * Set API data to registration metadata.
 *
 * @author Haotian Zhang
 */
public class TsfApiPolarisRegistrationCustomizer implements PolarisRegistrationCustomizer {
	private static final Logger LOG = LoggerFactory.getLogger(TsfApiPolarisRegistrationCustomizer.class);

	private static final String API_META_KEY = "TSF_API_METAS";
	private final ApplicationContext context;

	public TsfApiPolarisRegistrationCustomizer(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void customize(PolarisRegistration registration) {
		String apiMetaData = context.getEnvironment().getProperty("$api_metas");
		Map<String, Map<String, String>> metadata = registration.getExtendedMetadata();
		if (StringUtils.hasText(apiMetaData)) {
			if (!metadata.containsKey(SERVER_CONNECTOR_CONSUL)) {
				metadata.put(SERVER_CONNECTOR_CONSUL, new HashMap<>());
			}
			metadata.get(SERVER_CONNECTOR_CONSUL).put(API_META_KEY, apiMetaData);
		}
		else {
			LOG.warn("apiMetaData is null, service:{}", registration.getServiceId());
		}
	}
}
