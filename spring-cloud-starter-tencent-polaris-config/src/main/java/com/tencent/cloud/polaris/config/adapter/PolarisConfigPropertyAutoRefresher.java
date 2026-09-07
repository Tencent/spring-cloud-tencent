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

package com.tencent.cloud.polaris.config.adapter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.logger.PolarisConfigLoggerContext;
import com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.event.ConfigEvent;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.com.google.common.collect.Sets;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadGroupPolarisPropertySource;
import static com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils.loadPolarisPropertySource;

/**
 * 1. Listen to the Polaris server configuration publishing event 2. Write the changed
 * configuration content to propertySource 3. Refresh the context through contextRefresher
 *
 * @author lepdou
 */
public abstract class PolarisConfigPropertyAutoRefresher implements ApplicationListener<ApplicationReadyEvent>, PolarisConfigPropertyRefresher {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigPropertyAutoRefresher.class);
	private static final Set<String> registeredPolarisPropertySets = Sets.newConcurrentHashSet();
	/**
	 * Property keys contributed by encrypted config files. Values of these keys must never be
	 * written to logs in plain text.
	 * <p>
	 * Grow-only on purpose: once a key is known to be sensitive, keep masking it even after the
	 * encrypted file drops it. Over-masking only costs troubleshooting convenience, while
	 * under-masking is a leak.
	 */
	private static final Set<String> encryptedPropertyKeys = Sets.newConcurrentHashSet();
	private static final String FINGERPRINT_ALGORITHM = "SHA-256";
	/**
	 * Number of digest bytes kept in a fingerprint. 4 bytes are enough to tell two values apart.
	 */
	private static final int FINGERPRINT_BYTES = 4;
	/**
	 * Random per JVM: see {@link #fingerprint(String)} for why the digest must be salted.
	 */
	private static final byte[] FINGERPRINT_SALT = newFingerprintSalt();
	private final PolarisConfigProperties polarisConfigProperties;
	private final AtomicBoolean registered = new AtomicBoolean(false);
	// this class provides customized logic for some customers to configure special business group files
	private final PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer = PolarisServiceLoaderUtil.getPolarisConfigCustomExtensionLayer();
	private final ConfigFileService configFileService;
	private final SDKContext context;

	public PolarisConfigPropertyAutoRefresher(PolarisConfigProperties polarisConfigProperties,
			ConfigFileService configFileService, SDKContext context) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.configFileService = configFileService;
		this.context = context;
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
					PolarisPropertySource p = loadPolarisPropertySource(
							configFileService, configFileMetadata.getNamespace(),
							configFileMetadata.getFileGroup(), configFileMetadata.getFileName());
					LOGGER.info("[SCT Config] changed property = {}", p.getSource().keySet());
					changedKeys.addAll(p.getSource().keySet());
					this.registerPolarisConfigPublishChangeListener(p, polarisPropertySource);
					PolarisPropertySourceManager.addPropertySource(p);
					// the loaded file carries the per-file encrypted flag, so register its keys
					// right here rather than waiting for the first change event on that file
					markEncryptedKeys(p.getConfigKVFile());
					for (String changedKey : p.getSource().keySet()) {
						polarisPropertySource.getSource().put(changedKey, p.getSource().get(changedKey));
						refreshSpringValue(changedKey);
					}
				}
				refreshConfigurationProperties(changedKeys);

				reportEvent(added);
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

					// the change event is the only place carrying the plugin-level ConfigFile,
					// which is where the server-side per-file encrypted flag can be read
					markEncryptedKeys(listenPolarisPropertySource.getConfigKVFile(),
							configKVFileChangeEvent.getConfigFile(), configKVFileChangeEvent.changedKeys());

					Map<String, Object> effectSource = effectPolarisPropertySource.getSource();
					Map<String, Object> listenSource = listenPolarisPropertySource.getSource();
					boolean isGroupRefresh = !listenPolarisPropertySource.equals(effectPolarisPropertySource);

					PolarisPropertySource newGroupSource = null;
					if (isGroupRefresh) {
						newGroupSource = loadGroupPolarisPropertySource(configFileService,
								effectPolarisPropertySource.getNamespace(), effectPolarisPropertySource.getGroup());
					}

					for (String changedKey : configKVFileChangeEvent.changedKeys()) {
						ConfigPropertyChangeInfo configPropertyChangeInfo = configKVFileChangeEvent.getChangeInfo(changedKey);

						if (isEncryptedKey(changedKey)) {
							LOGGER.info("[SCT Config] changed property = [key={}, changeType={}, oldValue={}, newValue={}]",
									configPropertyChangeInfo.getPropertyName(), configPropertyChangeInfo.getChangeType(),
									maskValue(configPropertyChangeInfo.getOldValue()),
									maskValue(configPropertyChangeInfo.getNewValue()));
						}
						else {
							LOGGER.info("[SCT Config] changed property = {}", configPropertyChangeInfo);
						}

						// new ability to dynamically change log levels
						try {
							if (changedKey.startsWith("logging.level") && changedKey.length() >= 14) {
								String loggerName = changedKey.substring(14);
								String newValue = (String) configPropertyChangeInfo.getNewValue();
								// not masked even in an encrypted file: encryption is per file, and
								// the value here is a log level, never a secret
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

					reportEvent(listenPolarisPropertySource, configKVFileChangeEvent);

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

	/**
	 * Registers the property keys of an encrypted config file, so that their values can be masked
	 * in logs afterwards.
	 * <p>
	 * {@code configFile} must come from {@link ConfigKVFileChangeEvent#getConfigFile()}: that
	 * object belongs to the response chain, where {@code encrypted} is the per-file value pushed
	 * by the server. The request-side object is not usable as a criterion, because the crypto
	 * filter unconditionally sets it to true to declare crypto support.
	 *
	 * @param kvFile      the config file whose property names will be registered
	 * @param configFile  the plugin-level config file carrying the encrypted flag, may be null
	 * @param changedKeys keys from the change event; an ADDED key may not be in
	 *                    {@code kvFile.getPropertyNames()} yet
	 */
	private void markEncryptedKeys(ConfigKVFile kvFile, ConfigFile configFile, Set<String> changedKeys) {
		if (configFile == null || !configFile.isEncrypted()) {
			return;
		}
		if (kvFile != null) {
			Set<String> propertyNames = kvFile.getPropertyNames();
			if (!CollectionUtils.isEmpty(propertyNames)) {
				encryptedPropertyKeys.addAll(propertyNames);
			}
		}
		if (!CollectionUtils.isEmpty(changedKeys)) {
			encryptedPropertyKeys.addAll(changedKeys);
		}
	}

	/**
	 * Registers the property keys of a config file that reports itself as encrypted.
	 * <p>
	 * Used where no change event is available, e.g. a file newly added to a watched group.
	 * {@code ConfigKVFile#isEncrypted()} resolves to the server-pushed per-file flag, so it is
	 * usable from the very first load.
	 *
	 * @param kvFile the loaded config file, may be null
	 */
	private void markEncryptedKeys(ConfigKVFile kvFile) {
		if (kvFile == null || !kvFile.isEncrypted()) {
			return;
		}
		Set<String> propertyNames = kvFile.getPropertyNames();
		if (!CollectionUtils.isEmpty(propertyNames)) {
			encryptedPropertyKeys.addAll(propertyNames);
		}
	}

	/**
	 * @param key the property key
	 * @return whether the value of the given key comes from an encrypted config file
	 */
	protected boolean isEncryptedKey(String key) {
		return encryptedPropertyKeys.contains(key);
	}

	/**
	 * Masks a property value of an encrypted config file. The length and a fingerprint are kept as
	 * hints for troubleshooting, the content is not exposed.
	 * <p>
	 * Takes an Object rather than a String: both {@code ConfigPropertyChangeInfo#getOldValue()}
	 * and the resolved {@code @Value} result are declared as Object.
	 *
	 * @param value the raw value
	 * @return the masked value
	 */
	protected static String maskValue(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value);
		if (text.isEmpty()) {
			return "";
		}
		return "***(len=" + text.length() + ", fp=" + fingerprint(text) + ")";
	}

	/**
	 * Salted and truncated digest of a value, so that two masked values can be told apart even when
	 * their lengths are equal (e.g. an old and a new password of the same length).
	 * <p>
	 * The salt is random per JVM on purpose. An unsalted digest of a single config value would be
	 * reversible by dictionary attack, since config values carry little entropy - that would defeat
	 * the masking. With a per-process salt the fingerprint stays comparable within one log file,
	 * which is what change diagnosis needs, and carries no information outside it.
	 * <p>
	 * Truncated to 4 bytes: a collision only makes two different values look alike, it never
	 * exposes a value.
	 *
	 * @param text the raw value
	 * @return an 8-char hex fingerprint
	 */
	private static byte[] newFingerprintSalt() {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return salt;
	}

	private static String fingerprint(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance(FINGERPRINT_ALGORITHM);
			digest.update(FINGERPRINT_SALT);
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(FINGERPRINT_BYTES * 2);
			for (int i = 0; i < FINGERPRINT_BYTES; i++) {
				builder.append(Character.forDigit((hash[i] >> 4) & 0xF, 16));
				builder.append(Character.forDigit(hash[i] & 0xF, 16));
			}
			return builder.toString();
		}
		catch (NoSuchAlgorithmException e) {
			// SHA-256 is mandated by the JDK spec, so this is unreachable in practice
			return "unavailable";
		}
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

	private void reportEvent(PolarisPropertySource polarisPropertySource, ConfigKVFileChangeEvent configKVFileChangeEvent) {
		ConfigEvent.Builder builder = new ConfigEvent.Builder()
				.withTimestamp(LocalDateTime.now())
				.withEventType(EventConstants.EventType.CONFIG)
				.withEventName(EventConstants.EventName.ConfigUpdated)
				.withClientId(context.getExtensions().getValueContext().getClientId())
				.withClientIp(context.getExtensions().getValueContext().getHost())
				.withNamespace(polarisPropertySource.getNamespace())
				.withConfigGroup(polarisPropertySource.getGroup())
				.withConfigVersion(Optional.ofNullable(configKVFileChangeEvent.getConfigFile()).map(ConfigFile::getName)
						.orElse(null))
				.withConfigFileName(polarisPropertySource.getFileName());

		BaseFlow.reportConfigEvent(context.getExtensions(), builder.build());
	}

	private void reportEvent(Map<String, ConfigFileMetadata> added) {
		for (ConfigFileMetadata configFileMetadata : added.values()) {
			ConfigEvent.Builder builder = new ConfigEvent.Builder()
					.withTimestamp(LocalDateTime.now())
					.withEventType(EventConstants.EventType.CONFIG)
					.withEventName(EventConstants.EventName.ConfigUpdated)
					.withClientId(context.getExtensions().getValueContext().getClientId())
					.withClientIp(context.getExtensions().getValueContext().getHost())
					.withNamespace(configFileMetadata.getNamespace())
					.withConfigGroup(configFileMetadata.getFileGroup())
					.withConfigVersion(configFileMetadata.getFileVersion())
					.withConfigFileName(configFileMetadata.getFileName());

			BaseFlow.reportConfigEvent(context.getExtensions(), builder.build());
		}
	}

	/**
	 * Just for junit test.
	 */
	public void setRegistered(boolean registered) {
		this.registered.set(registered);
	}

	/**
	 * Just for junit test. {@code encryptedPropertyKeys} is static and grow-only, so it has to be
	 * reset between test methods.
	 */
	public static void clearEncryptedPropertyKeys() {
		encryptedPropertyKeys.clear();
	}
}
