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
 *
 */

package com.tencent.cloud.polaris.config.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import static com.tencent.polaris.configuration.api.core.ChangeType.ADDED;
import static com.tencent.polaris.configuration.api.core.ChangeType.DELETED;
import static com.tencent.polaris.configuration.api.core.ChangeType.MODIFIED;

/**
 * Polaris Config Listener Context Defined .
 * <p>This source file was reference from：
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/internals/AbstractConfig.java>
 *     AbstractConfig</a></code>
 *
 * @author Palmer Xu 2022-06-06
 */
public final class PolarisConfigListenerContext {

	/**
	 * Logger instance.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PolarisConfigListenerContext.class);
	/**
	 * Execute service Atomic Reference Cache .
	 */
	private static final AtomicReference<ExecutorService> EAR = new AtomicReference<>();
	/**
	 * All custom {@link ConfigChangeListener} instance defined in application .
	 */
	private static final List<ConfigChangeListener> listeners = Lists.newCopyOnWriteArrayList();
	/**
	 * All custom interested keys defined in application .
	 */
	private static final Map<ConfigChangeListener, Set<String>> interestedKeys = Maps.newHashMap();
	/**
	 * All custom interested key prefixes defined in application .
	 */
	private static final Map<ConfigChangeListener, Set<String>> interestedKeyPrefixes = Maps.newHashMap();
	/**
	 * Cache all latest configuration information for users in the application environment .
	 */
	private static final Cache<String, Object> properties = CacheBuilder.newBuilder().build();

	private PolarisConfigListenerContext() {
	}

	/**
	 * Get or Created new execute server .
	 * @return execute service instance of {@link ExecutorService}
	 */
	private static ExecutorService executor() {
		if (EAR.get() == null) {
			synchronized (PolarisConfigListenerContext.class) {
				int coreThreadSize = Runtime.getRuntime().availableProcessors();
				final ExecutorService service = new ThreadPoolExecutor(coreThreadSize, coreThreadSize,
						0, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<>(64),
						new CustomizableThreadFactory("Config-Change-Notify-Thread-Pool-"));

				// Register Jvm Shutdown Hook
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						LOG.info("Shutting down config change notify thread pool");
						service.shutdown();
					}
					catch (Exception ignore) {
					}
				}));
				EAR.set(service);
			}
		}
		return EAR.get();
	}

	/**
	 * Initialize Environment Properties cache after listen <code>ApplicationStartedEvent</code> event .
	 * @param ret origin properties map
	 */
	static void initialize(Map<String, Object> ret) {
		properties.putAll(ret);
	}

	/**
	 * Merge Changed Properties .
	 * @param ret current environment properties map
	 * @return merged properties result map
	 */
	static Map<String, ConfigPropertyChangeInfo> merge(Map<String, Object> ret) {
		Map<String, ConfigPropertyChangeInfo> changes = Maps.newHashMap();
		if (!ret.isEmpty()) {

			Map<String, Object> origin = Maps.newHashMap(properties.asMap());
			Map<String, ConfigPropertyChangeInfo> deleted = Maps.newHashMap();

			origin.keySet().parallelStream().forEach(key -> {
				if (!ret.containsKey(key)) {
					deleted.put(key, new ConfigPropertyChangeInfo(key, String.valueOf(origin.get(key)), null, DELETED));
					properties.invalidate(key);
				}
			});
			changes.putAll(deleted);

			ret.keySet().parallelStream().forEach(key -> {
				Object oldValue = properties.getIfPresent(key);
				Object newValue = ret.get(key);
				if (oldValue != null) {
					if (!newValue.equals(oldValue)) {
						properties.put(key, newValue);
						changes.put(key, new ConfigPropertyChangeInfo(key, String.valueOf(oldValue), String.valueOf(newValue), MODIFIED));
					}
				}
				else {
					properties.put(key, newValue);
					changes.put(key, new ConfigPropertyChangeInfo(key, null, String.valueOf(newValue), ADDED));
				}
			});
		}
		return changes;
	}

	/**
	 * Adding a config file change listener, will trigger a callback when the config file is published .
	 * @param listener the listener will be added
	 * @param interestedKeys the keys interested in the listener, will only be notified if any of the interested keys is changed.
	 * @param interestedKeyPrefixes the key prefixes that the listener is interested in,
	 *                                 will be notified if and only if the changed keys start with anyone of the prefixes.
	 */
	public static void addChangeListener(@NonNull ConfigChangeListener listener,
			@Nullable Set<String> interestedKeys, @Nullable Set<String> interestedKeyPrefixes) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			PolarisConfigListenerContext.interestedKeys.put(listener, interestedKeys == null ? Sets.newHashSet() : interestedKeys);
			PolarisConfigListenerContext.interestedKeyPrefixes.put(listener, interestedKeyPrefixes == null ? Sets.newHashSet() : interestedKeyPrefixes);
		}
	}

	/**
	 * Fire config change event with {@link  ConfigKVFileChangeListener} .
	 * @param changedKeys changed keys in listener
	 * @param changes target config file changes info
	 */
	public static void fireConfigChange(Set<String> changedKeys, Map<String, ConfigPropertyChangeInfo> changes) {
		final List<ConfigChangeListener> listeners = findMatchedConfigChangeListeners(changedKeys);
		for (ConfigChangeListener listener : listeners) {
			Set<String> interestedChangedKeys = resolveInterestedChangedKeys(listener, changedKeys);
			Map<String, ConfigPropertyChangeInfo> modifiedChanges = new HashMap<>(interestedChangedKeys.size());
			interestedChangedKeys.parallelStream().forEach(key -> modifiedChanges.put(key, changes.get(key)));
			ConfigChangeEvent event = new ConfigChangeEvent(modifiedChanges, interestedChangedKeys);
			PolarisConfigListenerContext.executor().execute(() -> listener.onChange(event));
		}
	}

	/**
	 * Try to find all matched config change listeners .
	 * @param changedKeys received changed keys
	 * @return list of matched {@link ConfigChangeListener}
	 */
	private static List<ConfigChangeListener> findMatchedConfigChangeListeners(Set<String> changedKeys) {
		final List<ConfigChangeListener> configChangeListeners = Lists.newArrayList();
		for (ConfigChangeListener listener : listeners) {
			if (isConfigChangeListenerInterested(listener, changedKeys)) {
				configChangeListeners.add(listener);
			}
		}
		return configChangeListeners;
	}

	/**
	 * Check {@link ConfigChangeListener} is interested in custom keys.
	 * @param listener instance of {@link ConfigChangeListener}
	 * @param changedKeys received changed keys
	 * @return true is interested in custom keys
	 */
	private static boolean isConfigChangeListenerInterested(ConfigChangeListener listener, Set<String> changedKeys) {
		Set<String> interestedKeys = PolarisConfigListenerContext.interestedKeys.get(listener);
		Set<String> interestedKeyPrefixes = PolarisConfigListenerContext.interestedKeyPrefixes.get(listener);

		if ((interestedKeys == null || interestedKeys.isEmpty())
				&& (interestedKeyPrefixes == null || interestedKeyPrefixes.isEmpty())) {
			return true;
		}

		if (interestedKeys != null) {
			for (String interestedKey : interestedKeys) {
				if (changedKeys.contains(interestedKey)) {
					return true;
				}
			}
		}

		if (interestedKeyPrefixes != null) {
			for (String prefix : interestedKeyPrefixes) {
				for (final String changedKey : changedKeys) {
					if (changedKey.startsWith(prefix)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Resolve all interested keys .
	 * @param listener instance of {@link ConfigChangeListener}
	 * @param changedKeys received changed keys
	 * @return set of all interested keys in listener
	 */
	private static Set<String> resolveInterestedChangedKeys(ConfigChangeListener listener, Set<String> changedKeys) {
		Set<String> interestedChangedKeys = Sets.newHashSet();

		if (interestedKeys.containsKey(listener)) {
			Set<String> interestedKeys = PolarisConfigListenerContext.interestedKeys.get(listener);
			for (String interestedKey : interestedKeys) {
				if (changedKeys.contains(interestedKey)) {
					interestedChangedKeys.add(interestedKey);
				}
			}
		}

		if (interestedKeyPrefixes.containsKey(listener)) {
			Set<String> interestedKeyPrefixes = PolarisConfigListenerContext.interestedKeyPrefixes.get(listener);
			for (String interestedKeyPrefix : interestedKeyPrefixes) {
				for (String changedKey : changedKeys) {
					if (changedKey.startsWith(interestedKeyPrefix)) {
						interestedChangedKeys.add(changedKey);
					}
				}
			}
		}

		return Collections.unmodifiableSet(interestedChangedKeys);
	}
}
