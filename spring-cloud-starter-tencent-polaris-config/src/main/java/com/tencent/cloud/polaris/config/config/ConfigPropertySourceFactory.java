package com.tencent.cloud.polaris.config.config;

import java.util.List;

import com.google.common.collect.Lists;

/**
 *@author : wh
 *@date : 2022/7/10 23:10
 *@description:
 */
public class ConfigPropertySourceFactory {

	private final List<ConfigPropertySource> configPropertySources = Lists.newLinkedList();

	public ConfigPropertySource getConfigPropertySource(String name, Config source) {
		ConfigPropertySource configPropertySource = new ConfigPropertySource(name, source);

		configPropertySources.add(configPropertySource);

		return configPropertySource;
	}

	public List<ConfigPropertySource> getAllConfigPropertySources() {
		return Lists.newLinkedList(configPropertySources);
	}
}
