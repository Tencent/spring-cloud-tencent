package com.tencent.cloud.polaris.config.config;

import java.util.Set;

import com.tencent.cloud.polaris.config.listener.ConfigChangeListener;

/**
 *@author : wh
 *@date : 2022/7/10 23:10
 *@description:
 */
public class ConfigPropertySource extends EnumerablePropertySource<Config> {
	private static final String[] EMPTY_ARRAY = new String[0];

	ConfigPropertySource(String name, Config source) {
		super(name, source);
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.getProperty(name, null) != null;
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> propertyNames = this.source.getPropertyNames();
		if (propertyNames.isEmpty()) {
			return EMPTY_ARRAY;
		}
		return propertyNames.toArray(new String[propertyNames.size()]);
	}

	@Override
	public Object getProperty(String name) {
		return this.source.getProperty(name, null);
	}

	public void addChangeListener(ConfigChangeListener listener) {
		this.source.addChangeListener(listener);
	}
}
