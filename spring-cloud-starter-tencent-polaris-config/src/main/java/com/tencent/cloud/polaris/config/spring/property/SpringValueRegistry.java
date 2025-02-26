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

package com.tencent.cloud.polaris.config.spring.property;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.tencent.polaris.api.pojo.TrieNode;
import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * Spring value auto registry.
 * <p>
 * This source file was originally from:
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/property/SpringValueRegistry.java>
 *     SpringValueRegistry</a></code>
 *
 * @author weihubeats 2022-7-10
 */
public class SpringValueRegistry implements DisposableBean {
	private static final Logger logger = LoggerFactory.getLogger(SpringValueRegistry.class);

	private static final long CLEAN_INTERVAL_IN_SECONDS = 5;
	private final Map<BeanFactory, Multimap<String, SpringValue>> registry = Maps.newConcurrentMap();
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final Object LOCK = new Object();
	private ScheduledExecutorService executor;

	private final TrieNode<String> refreshScopePrefixRoot = new TrieNode<>(TrieNode.ROOT_PATH);

	private final Set<String> refreshScopeKeys = Sets.newConcurrentHashSet();

	public void register(BeanFactory beanFactory, String key, SpringValue springValue) {
		if (!registry.containsKey(beanFactory)) {
			synchronized (LOCK) {
				if (!registry.containsKey(beanFactory)) {
					registry.put(beanFactory, Multimaps.synchronizedListMultimap(LinkedListMultimap.create()));
				}
			}
		}

		Multimap<String, SpringValue> multimap = registry.get(beanFactory);
		for (SpringValue existingValue : multimap.get(key)) {
			// if the spring value is already registered, remove it
			if (existingValue.getBeanName().equals(springValue.getBeanName())) {
				multimap.remove(key, existingValue);
				break;
			}

		}
		multimap.put(key, springValue);

		// lazy initialize
		if (initialized.compareAndSet(false, true)) {
			initialize();
		}
	}

	public Collection<SpringValue> get(BeanFactory beanFactory, String key) {
		Multimap<String, SpringValue> beanFactorySpringValues = registry.get(beanFactory);
		if (beanFactorySpringValues == null) {
			return null;
		}
		return beanFactorySpringValues.get(key);
	}

	private void initialize() {
		executor = Executors.newSingleThreadScheduledExecutor(
				new NamedThreadFactory("polaris-spring-value-registry"));
		executor.scheduleAtFixedRate(
				() -> {
					try {
						scanAndClean();
					}
					catch (Throwable ex) {
						logger.error(ex.getMessage(), ex);
					}
				}, CLEAN_INTERVAL_IN_SECONDS, CLEAN_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
	}

	private void scanAndClean() {
		Iterator<Multimap<String, SpringValue>> iterator = registry.values().iterator();
		while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
			Multimap<String, SpringValue> springValues = iterator.next();
			// clear unused spring values
			springValues.entries().removeIf(springValue -> !springValue.getValue().isTargetBeanValid());
		}
	}

	public void putRefreshScopePrefixKey(String key) {
		TrieUtil.buildConfigTrieNode(key, refreshScopePrefixRoot);
	}

	public void putRefreshScopeKey(String key) {
		refreshScopeKeys.add(key);
	}

	public void putRefreshScopeKeys(Set<String> keys) {
		refreshScopeKeys.addAll(keys);
	}

	/**
	 * first check if the key is in refreshScopeKeys, if not, check the key by TrieUtil.
	 * @param key changed key.
	 * @return true if the key is refresh scope key, otherwise false.
	 */
	public boolean isRefreshScopeKey(String key) {
		if (refreshScopeKeys.contains(key)) {
			return true;
		}
		return TrieUtil.checkConfig(refreshScopePrefixRoot, key);
	}

	@Override
	public void destroy() throws Exception {
		executor.shutdown();
	}
}
