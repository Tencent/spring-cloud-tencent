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
package com.tencent.cloud.polaris.config.config;

import java.util.List;

/**
 * the config file group.
 *
 * @author lepdou 2022-03-28
 */
public class ConfigFileGroup {

	private String namespace;

	/**
	 * group name.
	 */
	private String name;

	/**
	 * the files belong to group.
	 */
	private List<String> files;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	@Override
	public String toString() {
		return "ConfigFileGroup{" +
				"namespace='" + namespace + '\'' +
				", name='" + name + '\'' +
				", files=" + files +
				'}';
	}
}
