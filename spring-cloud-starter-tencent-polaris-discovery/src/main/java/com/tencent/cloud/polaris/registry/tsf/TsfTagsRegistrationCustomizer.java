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

package com.tencent.cloud.polaris.registry.tsf;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfCoreProperties;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisRegistrationCustomizer;
import com.tencent.polaris.api.utils.StringUtils;

import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.TAGS_KEY;

/**
 * @author Haotian Zhang
 */
public class TsfTagsRegistrationCustomizer implements PolarisRegistrationCustomizer {

	private final TsfCoreProperties tsfCoreProperties;

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	public TsfTagsRegistrationCustomizer(TsfCoreProperties tsfCoreProperties, PolarisDiscoveryProperties polarisDiscoveryProperties) {
		this.tsfCoreProperties = tsfCoreProperties;
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
	}

	@Override
	public void customize(PolarisRegistration registration) {
		if (tsfCoreProperties == null) {
			return;
		}

		String protocol = ApplicationContextAwareUtils.getProperties("tsf.discovery.scheme",
				ApplicationContextAwareUtils.getProperties("spring.cloud.polaris.discovery.protocol", "http"));
		if (StringUtils.isNotBlank(protocol)) {
			tsfCoreProperties.setScheme(protocol);
			polarisDiscoveryProperties.setProtocol(protocol);
		}
		Map<String, String> metadata = registration.getExtendedMetadata()
				.computeIfAbsent(TAGS_KEY, k -> new HashMap<>());
		for (String tag : tsfCoreProperties.getTsfTags()) {
			metadata.put(TAGS_KEY, tag);
		}
	}
}
