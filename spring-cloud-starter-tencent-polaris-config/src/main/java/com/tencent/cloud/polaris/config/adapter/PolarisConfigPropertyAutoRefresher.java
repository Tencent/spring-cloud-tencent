/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

/**
 * 1. Listen to the Polaris server configuration publishing event 2. Write the changed
 * configuration content to propertySource 3. Refresh the context through contextRefresher
 *
 * @author lepdou 2022-03-28
 */
public abstract class PolarisConfigPropertyAutoRefresher
		implements ApplicationListener<ApplicationReadyEvent>, PolarisConfigPropertyRefresher {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigPropertyAutoRefresher.class);

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisPropertySourceManager polarisPropertySourceManager;

	private final AtomicBoolean registered = new AtomicBoolean(false);

	public PolarisConfigPropertyAutoRefresher(
			PolarisConfigProperties polarisConfigProperties,
			PolarisPropertySourceManager polarisPropertySourceManager) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisPropertySourceManager = polarisPropertySourceManager;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
		registerPolarisConfigPublishEvent();
	}

	private void registerPolarisConfigPublishEvent() {
		if (!polarisConfigProperties.isAutoRefresh()) {
			return;
		}

		List<PolarisPropertySource> polarisPropertySources = polarisPropertySourceManager.getAllPropertySources();
		if (CollectionUtils.isEmpty(polarisPropertySources)) {
			return;
		}

		if (!registered.compareAndSet(false, true)) {
			return;
		}

		// register polaris config publish event
		for (PolarisPropertySource polarisPropertySource : polarisPropertySources) {
			polarisPropertySource.getConfigKVFile()
					.addChangeListener((ConfigKVFileChangeListener) configKVFileChangeEvent -> {

						LOGGER.info(
								"[SCT Config] received polaris config change event and will refresh spring context."
										+ " namespace = {}, group = {}, fileName = {}",
								polarisPropertySource.getNamespace(),
								polarisPropertySource.getGroup(),
								polarisPropertySource.getFileName());

						Map<String, Object> source = polarisPropertySource.getSource();

						for (String changedKey : configKVFileChangeEvent.changedKeys()) {
							ConfigPropertyChangeInfo configPropertyChangeInfo = configKVFileChangeEvent
									.getChangeInfo(changedKey);

							LOGGER.info("[SCT Config] changed property = {}", configPropertyChangeInfo);

							switch (configPropertyChangeInfo.getChangeType()) {
							case MODIFIED:
							case ADDED:
								source.put(changedKey, configPropertyChangeInfo.getNewValue());
								break;
							case DELETED:
								source.remove(changedKey);
								break;
							}
							// update the attribute with @Value annotation
							refreshSpringValue(changedKey);
						}
						// update @ConfigurationProperties beans
						refreshConfigurationProperties(configKVFileChangeEvent.changedKeys());
					});
		}
	}
}
