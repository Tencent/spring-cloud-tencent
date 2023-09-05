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

import java.util.Map;

import com.tencent.polaris.configuration.api.core.ConfigKVFile;

import org.springframework.core.env.MapPropertySource;

/**
 * a polaris config file will be wrapped as polaris property source.
 *
 * @author lepdou 2022-03-10
 */
public class PolarisPropertySource extends MapPropertySource {

	private final String namespace;

	private final String group;

	private final String fileName;

	private final ConfigKVFile configKVFile;

	public PolarisPropertySource(String namespace, String group, String fileName, ConfigKVFile configKVFile, Map<String, Object> source) {
		super(namespace + "-" + group + "-" + fileName, source);

		this.namespace = namespace;
		this.group = group;
		this.fileName = fileName;
		this.configKVFile = configKVFile;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getGroup() {
		return group;
	}

	public String getFileName() {
		return fileName;
	}

	public String getPropertySourceName() {
		return namespace + "-" + group + "-" + fileName;
	}

	ConfigKVFile getConfigKVFile() {
		return configKVFile;
	}

	@Override
	public String toString() {
		return "PolarisPropertySource{" + "namespace='" + namespace + '\'' + ", group='" + group + '\'' + ", fileName='" + fileName + '\'' + '}';
	}
}
