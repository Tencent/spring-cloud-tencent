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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKeyConflict;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.bootstrap.config.BootstrapPropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Resolves key list, file value, effective value and conflict context from Spring
 * {@link ConfigurableEnvironment}, for polaris-java config effective-time query.
 * <p>
 * Config values must never be written to any log (including DEBUG): exception messages
 * and stack traces may embed raw values (e.g. Spring's unresolvable-placeholder error
 * quotes the whole value), so logs only carry key names, file coordinates and exception
 * class names. Per the interface contract, methods must not throw: single-key failures
 * degrade that key only.
 *
 * @author evelynwei
 */
public class SpringConfigEffectiveValueProvider implements ConfigEffectiveValueProvider {

	private static final Logger LOG = LoggerFactory.getLogger(SpringConfigEffectiveValueProvider.class);

	private static final String SOURCE_PREFIX = "polaris:";

	/**
	 * Name of Spring Boot's configuration-properties facade source
	 * ({@code ConfigurationPropertySourcesPropertySource}). Attached at the head of the
	 * Environment, its containsProperty delegates to every underlying source, so it
	 * matches any key and would shadow the real source.
	 */
	private static final String CONFIGURATION_PROPERTIES_SOURCE_NAME = "configurationProperties";

	private final ConfigurableEnvironment environment;

	public SpringConfigEffectiveValueProvider(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public List<String> getKeys(ConfigFileMetadata configFile) {
		try {
			ConfigKVFile file = findConfigKVFile(configFile);
			if (file == null) {
				return Collections.emptyList();
			}
			List<String> keys = new ArrayList<>(file.getPropertyNames());
			// Sort for stable output, friendly to tests and console display.
			Collections.sort(keys);
			return keys;
		}
		catch (Throwable t) {
			LOG.warn("[SCT Config] Get keys failed, file = {}, error = {}", coordinateOf(configFile),
					t.getClass().getSimpleName());
			return Collections.emptyList();
		}
	}

	@Override
	public EffectiveValue resolve(String key, ConfigFileMetadata configFile) {
		String fileValue;
		try {
			ConfigKVFile file = findConfigKVFile(configFile);
			// Raw value in the file (may contain unresolved placeholders).
			fileValue = file == null ? null : file.getProperty(key, null);
		}
		catch (Throwable t) {
			// Contract: return null instead of throwing; SDK degrades this key only.
			LOG.warn("[SCT Config] Resolve file value failed, key = {}, file = {}, error = {}", key,
					coordinateOf(configFile), t.getClass().getSimpleName());
			return null;
		}
		String effectiveValue = null;
		String propertySource = null;
		try {
			// Effective value: Environment has already converged by precedence and
			// resolves ${} placeholders, i.e. the value the application actually reads.
			effectiveValue = environment.getProperty(key);
			propertySource = resolvePropertySourceName(key);
		}
		catch (Throwable t) {
			// Only degrade the effective-value dimension; the file value already in hand is returned.
			LOG.warn("[SCT Config] Resolve effective value failed, key = {}, error = {}", key,
					t.getClass().getSimpleName());
		}
		return new EffectiveValue(fileValue, effectiveValue, propertySource);
	}

	@Override
	public List<ConfigKeyConflict> resolveConflicts(String key, ConfigFileMetadata excludeFile) {
		List<ConfigKeyConflict> conflicts = new ArrayList<>();
		// Static call: PolarisPropertySourceManager is not a bean and cannot be injected.
		// Used here only to enumerate the watched-file set, never for precedence.
		for (PolarisPropertySource source : PolarisPropertySourceManager.getAllPropertySources()) {
			for (ConfigKVFile file : expandSubs(source.getConfigKVFile())) {
				try {
					collectIfConflict(conflicts, key, file, excludeFile);
				}
				catch (Throwable t) {
					// Per-file fallback: one broken file must not drop conflicts already collected.
					LOG.warn("[SCT Config] Resolve conflicts failed, key = {}, error = {}", key,
							t.getClass().getSimpleName());
				}
			}
		}
		return conflicts;
	}

	/**
	 * Walks the ordered PropertySource chain of the Environment and returns the source
	 * identity of the first one containing the key. Iteration order of
	 * MutablePropertySources is exactly Spring's precedence order.
	 */
	private String resolvePropertySourceName(String key) {
		for (PropertySource<?> source : environment.getPropertySources()) {
			String name = matchSource(source, key);
			if (name != null) {
				return name;
			}
		}
		return null;
	}

	/**
	 * Matches the key inside a single PropertySource, unwrapping wrappers first:
	 * in bootstrap mode polaris sources are wrapped into a CompositePropertySource
	 * ("polaris-config") and then into BootstrapPropertySource, so recursion is needed
	 * to reach the real PolarisPropertySource. Polaris sources are translated to a
	 * normalized coordinate {@code polaris:namespace/group/fileName}.
	 */
	private String matchSource(PropertySource<?> source, String key) {
		if (CONFIGURATION_PROPERTIES_SOURCE_NAME.equals(source.getName())) {
			return null;
		}
		if (source instanceof CompositePropertySource) {
			for (PropertySource<?> sub : ((CompositePropertySource) source).getPropertySources()) {
				String name = matchSource(sub, key);
				if (name != null) {
					return name;
				}
			}
			return null;
		}
		if (source instanceof BootstrapPropertySource) {
			return matchSource(((BootstrapPropertySource<?>) source).getDelegate(), key);
		}
		if (!source.containsProperty(key)) {
			return null;
		}
		if (source instanceof PolarisPropertySource) {
			PolarisPropertySource polarisSource = (PolarisPropertySource) source;
			ConfigKVFile file = polarisSource.getConfigKVFile();
			if (file instanceof CompositeConfigFile) {
				// Group-dimension load: merge semantics is first-file-wins, so the first sub
				// file containing the key is the effective one.
				for (ConfigKVFile sub : ((CompositeConfigFile) file).getConfigKVFiles()) {
					if (sub.getProperty(key, null) != null) {
						return formatCoordinate(sub);
					}
				}
				// Fallback: the composite's sub list is frozen at startup and does not cover
				// files added to the group at runtime; look them up by group coordinate.
				ConfigKVFile added = findInGroup(polarisSource.getNamespace(), polarisSource.getGroup(), key);
				if (added != null) {
					return formatCoordinate(added);
				}
			}
			else {
				return formatCoordinate(file);
			}
		}
		return source.getName();
	}

	/**
	 * Finds the watched ConfigKVFile by coordinate. Sources loaded by file dimension match
	 * directly; sources loaded by group dimension carry an empty fileName and their
	 * CompositeConfigFile must be expanded to match sub files.
	 */
	private ConfigKVFile findConfigKVFile(ConfigFileMetadata metadata) {
		for (PolarisPropertySource source : PolarisPropertySourceManager.getAllPropertySources()) {
			for (ConfigKVFile file : expandSubs(source.getConfigKVFile())) {
				if (sameFile(file, metadata)) {
					return file;
				}
			}
		}
		return null;
	}

	/**
	 * Finds a watched file in the given namespace/group containing the key. Used as the
	 * fallback for group sources whose frozen composite misses runtime-added files.
	 */
	private ConfigKVFile findInGroup(String namespace, String group, String key) {
		for (PolarisPropertySource source : PolarisPropertySourceManager.getAllPropertySources()) {
			for (ConfigKVFile file : expandSubs(source.getConfigKVFile())) {
				if (Objects.equals(file.getNamespace(), namespace) && Objects.equals(file.getFileGroup(), group)
						&& file.getProperty(key, null) != null) {
					return file;
				}
			}
		}
		return null;
	}

	/**
	 * Expands a CompositeConfigFile to its sub files; a plain file expands to itself.
	 */
	private List<ConfigKVFile> expandSubs(ConfigKVFile file) {
		if (file instanceof CompositeConfigFile) {
			List<ConfigKVFile> subs = ((CompositeConfigFile) file).getConfigKVFiles();
			return subs == null ? Collections.emptyList() : subs;
		}
		return Collections.singletonList(file);
	}

	private void collectIfConflict(List<ConfigKeyConflict> conflicts, String key, ConfigKVFile candidate,
			ConfigFileMetadata excludeFile) {
		if (sameFile(candidate, excludeFile)) {
			return;
		}
		String value = candidate.getProperty(key, null);
		if (value == null) {
			return;
		}
		// The same file may be watched at both file dimension and group dimension; dedupe by coordinate.
		for (ConfigKeyConflict existing : conflicts) {
			if (Objects.equals(existing.getNamespace(), candidate.getNamespace())
					&& Objects.equals(existing.getGroup(), candidate.getFileGroup())
					&& Objects.equals(existing.getFileName(), candidate.getFileName())) {
				return;
			}
		}
		// Coordinate + value only; never the full content of the conflict file.
		conflicts.add(new ConfigKeyConflict(candidate.getNamespace(), candidate.getFileGroup(),
				candidate.getFileName(), value));
	}

	private boolean sameFile(ConfigKVFile candidate, ConfigFileMetadata metadata) {
		return metadata != null
				&& Objects.equals(candidate.getNamespace(), metadata.getNamespace())
				&& Objects.equals(candidate.getFileGroup(), metadata.getFileGroup())
				&& Objects.equals(candidate.getFileName(), metadata.getFileName());
	}

	private String formatCoordinate(ConfigKVFile file) {
		return SOURCE_PREFIX + file.getNamespace() + "/" + file.getFileGroup() + "/" + file.getFileName();
	}

	private String coordinateOf(ConfigFileMetadata metadata) {
		if (metadata == null) {
			return "null";
		}
		return metadata.getNamespace() + "/" + metadata.getFileGroup() + "/" + metadata.getFileName();
	}
}
