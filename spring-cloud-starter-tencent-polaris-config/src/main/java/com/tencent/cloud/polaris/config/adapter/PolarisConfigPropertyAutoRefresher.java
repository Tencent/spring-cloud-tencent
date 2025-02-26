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

package com.tencent.cloud.polaris.config.adapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.logger.PolarisConfigLoggerContext;
import com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

/**
 * 1. Listen to the Polaris server configuration publishing event 2. Write the changed
 * configuration content to propertySource 3. Refresh the context through contextRefresher
 *
 * @author lepdou
 */
public abstract class PolarisConfigPropertyAutoRefresher implements ApplicationListener<ApplicationReadyEvent>, PolarisConfigPropertyRefresher {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigPropertyAutoRefresher.class);

	private final PolarisConfigProperties polarisConfigProperties;

	private final AtomicBoolean registered = new AtomicBoolean(false);

	// this class provides customized logic for some customers to configure special business group files
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();

	private static final Set<String> registeredPolarisPropertySets = Sets.newConcurrentHashSet();

	private final ConfigFileService configFileService;

	public PolarisConfigPropertyAutoRefresher(PolarisConfigProperties polarisConfigProperties,
			ConfigFileService configFileService) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.configFileService = configFileService;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
		registerPolarisConfigPublishEvent();
	}

	private void registerPolarisConfigPublishEvent() {
		if (!polarisConfigProperties.isAutoRefresh()) {
			return;
		}

		List<PolarisPropertySource> polarisPropertySources = PolarisPropertySourceManager.getAllPropertySources();
		if (CollectionUtils.isEmpty(polarisPropertySources)) {
			return;
		}

		if (!registered.compareAndSet(false, true)) {
			return;
		}

		// custom register polaris config
		customInitRegisterPolarisConfig(this);

		// register polaris config publish event
		for (PolarisPropertySource polarisPropertySource : polarisPropertySources) {
			// group property source
			if (polarisPropertySource.getConfigKVFile() instanceof CompositeConfigFile) {
				CompositeConfigFile configKVFile = (CompositeConfigFile) polarisPropertySource.getConfigKVFile();
				for (ConfigKVFile cf : configKVFile.getConfigKVFiles()) {
					PolarisPropertySource p = new PolarisPropertySource(cf.getNamespace(), cf.getFileGroup(), cf.getFileName(), cf, new HashMap<>());
					registerPolarisConfigPublishChangeListener(p, polarisPropertySource);
					customRegisterPolarisConfigPublishChangeListener(p, polarisPropertySource);
					registeredPolarisPropertySets.add(p.getPropertySourceName());
				}
				registerPolarisConfigGroupChangeListener(polarisPropertySource);
			}
			else {
				registerPolarisConfigPublishChangeListener(polarisPropertySource);
				customRegisterPolarisConfigPublishChangeListener(polarisPropertySource);
			}
		}
	}

	private void customInitRegisterPolarisConfig(PolarisConfigPropertyAutoRefresher polarisConfigPropertyAutoRefresher) {
		if (polarisConfigCustomExtensionLayer == null) {
			LOGGER.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.initRegisterConfig(polarisConfigPropertyAutoRefresher);
	}

	private void registerPolarisConfigGroupChangeListener(PolarisPropertySource polarisPropertySource) {
		ConfigFileGroup configFileGroup = configFileService.getConfigFileGroup(
				polarisPropertySource.getNamespace(), polarisPropertySource.getGroup());

		if (configFileGroup == null) {
			return;
		}
		configFileGroup.addChangeListener(event -> {
			try {
				LOGGER.debug("ConfigFileGroup receive onChange event:{}", event);
				List<ConfigFileMetadata> oldConfigFileMetadataList = event.getOldConfigFileMetadataList();
				List<ConfigFileMetadata> newConfigFileMetadataList = event.getNewConfigFileMetadataList();

				Map<String, ConfigFileMetadata> added = calculateUnregister(oldConfigFileMetadataList, newConfigFileMetadataList);
				if (added.isEmpty()) {
					return;
				}
				Set<String> changedKeys = new HashSet<>();

				for (Map.Entry<String, ConfigFileMetadata> entry : added.entrySet()) {
					if (registeredPolarisPropertySets.contains(entry.getKey())) {
						continue;
					}
					registeredPolarisPropertySets.add(entry.getKey());
					LOGGER.info("[SCT Config] add polaris config file:{}", entry.getKey());
					ConfigFileMetadata configFileMetadata = entry.getValue();
					PolarisPropertySource p = PolarisConfigFileLocator.loadPolarisPropertySource(
							configFileService, configFileMetadata.getNamespace(),
							configFileMetadata.getFileGroup(), configFileMetadata.getFileName());
					LOGGER.info("[SCT Config] changed property = {}", p.getSource().keySet());
					changedKeys.addAll(p.getSource().keySet());
					this.registerPolarisConfigPublishChangeListener(p, polarisPropertySource);
					PolarisPropertySourceManager.addPropertySource(p);
					for (String changedKey : p.getSource().keySet()) {
						polarisPropertySource.getSource().put(changedKey, p.getSource().get(changedKey));
						refreshSpringValue(changedKey);
					}
				}
				refreshConfigurationProperties(changedKeys);
			}
			catch (Exception e) {
				LOGGER.error("[SCT Config] receive onChange exception,", e);
			}
		});
	}

	public void registerPolarisConfigPublishChangeListener(PolarisPropertySource polarisPropertySource) {
		registerPolarisConfigPublishChangeListener(polarisPropertySource, polarisPropertySource);
	}

	public void registerPolarisConfigPublishChangeListener(PolarisPropertySource listenPolarisPropertySource, PolarisPropertySource effectPolarisPropertySource) {
		LOGGER.info("{} will register polaris config publish listener, effect source:{}",
				listenPolarisPropertySource.getPropertySourceName(), effectPolarisPropertySource.getPropertySourceName());
		listenPolarisPropertySource.getConfigKVFile()
				.addChangeListener((ConfigKVFileChangeListener) configKVFileChangeEvent -> {

					LOGGER.info("[SCT Config] received polaris config change event and will refresh spring context." + " namespace = {}, group = {}, fileName = {}",
							listenPolarisPropertySource.getNamespace(), listenPolarisPropertySource.getGroup(), listenPolarisPropertySource.getFileName());

					Map<String, Object> effectSource = effectPolarisPropertySource.getSource();
					Map<String, Object> listenSource = listenPolarisPropertySource.getSource();
					boolean isGroupRefresh = !listenPolarisPropertySource.equals(effectPolarisPropertySource);

					PolarisPropertySource newGroupSource = null;
					if (isGroupRefresh) {
						newGroupSource = PolarisConfigFileLocator.loadGroupPolarisPropertySource(configFileService,
								effectPolarisPropertySource.getNamespace(), effectPolarisPropertySource.getGroup());
					}

					for (String changedKey : configKVFileChangeEvent.changedKeys()) {
						ConfigPropertyChangeInfo configPropertyChangeInfo = configKVFileChangeEvent.getChangeInfo(changedKey);

						LOGGER.info("[SCT Config] changed property = {}", configPropertyChangeInfo);

						// new ability to dynamically change log levels
						try {
							if (changedKey.startsWith("logging.level") && changedKey.length() >= 14) {
								String loggerName = changedKey.substring(14);
								String newValue = (String) configPropertyChangeInfo.getNewValue();
								LOGGER.info("[SCT Config] set logging.level loggerName:{}, newValue:{}", loggerName, newValue);
								PolarisConfigLoggerContext.setLevel(loggerName, newValue);
							}
						}
						catch (Exception e) {
							LOGGER.error("[SCT Config] set logging.level exception,", e);
						}
						switch (configPropertyChangeInfo.getChangeType()) {
						case MODIFIED:
						case ADDED:
							effectSource.put(changedKey, configPropertyChangeInfo.getNewValue());
							if (isGroupRefresh) {
								listenSource.put(changedKey, configPropertyChangeInfo.getNewValue());
							}
							break;
						case DELETED:
							if (isGroupRefresh) {
								// when the key is deleted, the value should load from group source
								Object newValue = Optional.ofNullable(newGroupSource).map(PropertySource::getSource).
										map(source -> source.get(changedKey)).orElse(null);
								if (newValue != null) {
									effectSource.put(changedKey, newValue);
								}
								else {
									effectSource.remove(changedKey);
								}
								listenSource.remove(changedKey);
							}
							else {
								effectSource.remove(changedKey);
							}
							break;
						}
						// update the attribute with @Value annotation
						refreshSpringValue(changedKey);
					}
					// update @ConfigurationProperties beans
					refreshConfigurationProperties(configKVFileChangeEvent.changedKeys());
				});
	}

	private void customRegisterPolarisConfigPublishChangeListener(PolarisPropertySource polarisPropertySource) {
		customRegisterPolarisConfigPublishChangeListener(polarisPropertySource, polarisPropertySource);
	}

	private void customRegisterPolarisConfigPublishChangeListener(PolarisPropertySource listenPolarisPropertySource, PolarisPropertySource effectPolarisPropertySource) {
		if (polarisConfigCustomExtensionLayer == null) {
			LOGGER.debug("[SCT Config] PolarisConfigCustomExtensionLayer is not init, ignore the following execution steps");
			return;
		}
		polarisConfigCustomExtensionLayer.executeRegisterPublishChangeListener(listenPolarisPropertySource, effectPolarisPropertySource);
	}

	private Map<String, ConfigFileMetadata> calculateUnregister(List<ConfigFileMetadata> oldConfigFileMetadataList,
			List<ConfigFileMetadata> newConfigFileMetadataList) {


		Map<String, ConfigFileMetadata> oldConfigFileMetadataMap = oldConfigFileMetadataList.stream()
				.collect(Collectors.toMap(
						configFileMetadata -> PolarisPropertySourceUtils.generateName(
								configFileMetadata.getNamespace(),
								configFileMetadata.getFileGroup(),
								configFileMetadata.getFileName()),
						configFileMetadata -> configFileMetadata));

		Map<String, ConfigFileMetadata> newConfigFileMetadataMap = newConfigFileMetadataList.stream()
				.collect(Collectors.toMap(
						configFileMetadata -> PolarisPropertySourceUtils.generateName(
								configFileMetadata.getNamespace(),
								configFileMetadata.getFileGroup(),
								configFileMetadata.getFileName()),
						configFileMetadata -> configFileMetadata));
		Map<String, ConfigFileMetadata> added = new HashMap<>();
		for (Map.Entry<String, ConfigFileMetadata> entry : newConfigFileMetadataMap.entrySet()) {
			if (!oldConfigFileMetadataMap.containsKey(entry.getKey())) {
				added.put(entry.getKey(), entry.getValue());
			}
		}
		return added;
	}

	/**
	 * Just for junit test.
	 */
	public void setRegistered(boolean registered) {
		this.registered.set(registered);
	}
}
