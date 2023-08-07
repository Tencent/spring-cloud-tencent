/*
 * Copyright (c) 2023 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */
package com.tencent.cloud.polaris.config.adapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;

/**
 * @author juanyinyang
 */
public final class PolarisContextRefreshUtil {

	// 最近一次的全量PolarisPropertySource集合（PolarisPropertySource按 namespace + fileGroup + fileName 确保唯一）
	private static final Map<PolarisPropertySource, ConfigKVFileChangeListener> lastPolarisPropertyConfigKVFileMap = new LinkedHashMap<>();
	// 命名空间分组（namespace + fileGroup）的去重Set集合，如果这个分组已添加了ConfigFileGroupListener
	private static final Set<String> existConfigFileGroupListenerSet = new ConcurrentHashSet<>();
	// Queue里存放的是需要添加的配置列表集合（这类配置需要重新注册Listener）
	private static final LinkedBlockingQueue<List<PolarisPropertySource>> registerPolarisPropertySourceQueue = new LinkedBlockingQueue<>(100);

	private PolarisContextRefreshUtil() {

	}

	public static Map<PolarisPropertySource, ConfigKVFileChangeListener> getLastPolarisPropertyConfigKVFileMap() {
		return lastPolarisPropertyConfigKVFileMap;
	}

	public static Set<String> getExistConfigFileGroupListenerSet() {
		return existConfigFileGroupListenerSet;
	}

	public static LinkedBlockingQueue<List<PolarisPropertySource>> getRegisterPolarisPropertySourceQueue() {
		return registerPolarisPropertySourceQueue;
	}
}
