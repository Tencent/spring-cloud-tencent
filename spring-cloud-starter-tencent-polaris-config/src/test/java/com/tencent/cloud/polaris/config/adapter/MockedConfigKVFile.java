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

package com.tencent.cloud.polaris.config.adapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.polaris.configuration.api.core.ConfigFileChangeListener;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeListener;

/**
 * Mock config kv file for test.
 *
 * @author lepdou 2022-06-11
 */
public class MockedConfigKVFile implements ConfigKVFile {

	private final Map<String, Object> properties;
	private final List<ConfigKVFileChangeListener> listeners = new ArrayList<>();

	public MockedConfigKVFile(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public String getProperty(String s, String s1) {
		return String.valueOf(properties.get(s));
	}

	@Override
	public Integer getIntProperty(String s, Integer integer) {
		return null;
	}

	@Override
	public Long getLongProperty(String s, Long aLong) {
		return null;
	}

	@Override
	public Short getShortProperty(String s, Short aShort) {
		return null;
	}

	@Override
	public Float getFloatProperty(String s, Float aFloat) {
		return null;
	}

	@Override
	public Double getDoubleProperty(String s, Double aDouble) {
		return null;
	}

	@Override
	public Byte getByteProperty(String s, Byte aByte) {
		return null;
	}

	@Override
	public Boolean getBooleanProperty(String s, Boolean aBoolean) {
		return null;
	}

	@Override
	public String[] getArrayProperty(String s, String s1, String[] strings) {
		return new String[0];
	}

	@Override
	public <T extends Enum<T>> T getEnumProperty(String s, Class<T> aClass, T t) {
		return null;
	}

	@Override
	public <T> T getJsonProperty(String s, Class<T> aClass, T t) {
		return null;
	}

	@Override
	public <T> T getJsonProperty(String s, Type type, T t) {
		return null;
	}

	@Override
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public void addChangeListener(ConfigKVFileChangeListener configKVFileChangeListener) {
		listeners.add(configKVFileChangeListener);
	}

	@Override
	public void removeChangeListener(ConfigKVFileChangeListener configKVFileChangeListener) {

	}

	@Override
	public String getContent() {
		return null;
	}

	@Override
	public <T> T asJson(Class<T> aClass, T t) {
		return null;
	}

	@Override
	public <T> T asJson(Type type, T t) {
		return null;
	}

	@Override
	public boolean hasContent() {
		return false;
	}

	@Override
	public String getMd5() {
		return null;
	}

	@Override
	public void addChangeListener(ConfigFileChangeListener configFileChangeListener) {

	}

	@Override
	public void removeChangeListener(ConfigFileChangeListener configFileChangeListener) {

	}

	public void fireChangeListener(ConfigKVFileChangeEvent event) {
		for (ConfigKVFileChangeListener listener : listeners) {
			listener.onChange(event);
		}
	}

	@Override
	public String getNamespace() {
		return null;
	}

	@Override
	public String getFileGroup() {
		return null;
	}

	@Override
	public String getFileName() {
		return null;
	}
}
