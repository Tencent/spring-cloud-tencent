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

package com.tencent.cloud.polaris.config.enums;

/**
 * the format of config file.
 *
 * @author lepdou 2022-03-28
 */
public enum ConfigFileFormat {

	/**
	 * property format.
	 */
	PROPERTY(".properties"),
	/**
	 * yaml format.
	 */
	YAML(".yaml"),
	/**
	 * yml format.
	 */
	YML(".yml"),
	/**
	 * xml format.
	 */
	XML(".xml"),
	/**
	 * json format.
	 */
	JSON(".json"),
	/**
	 * text format.
	 */
	TEXT(".txt"),
	/**
	 * html format.
	 */
	html(".html"),
	/**
	 * unknown format.
	 */
	UNKNOWN(".unknown");

	private final String extension;

	ConfigFileFormat(String extension) {
		this.extension = extension;
	}

	public static boolean isPropertyFile(String fileName) {
		return fileName.endsWith(PROPERTY.extension);
	}

	public static boolean isYamlFile(String fileName) {
		return fileName.endsWith(YAML.extension) || fileName.endsWith(YML.extension);
	}

	public static boolean isUnknownFile(String fileName) {
		for (ConfigFileFormat format : ConfigFileFormat.values()) {
			if (fileName.endsWith(format.extension)) {
				return false;
			}
		}
		return true;
	}

}
