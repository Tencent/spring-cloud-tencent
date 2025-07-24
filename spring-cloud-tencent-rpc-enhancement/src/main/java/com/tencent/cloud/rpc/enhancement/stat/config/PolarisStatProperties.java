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

package com.tencent.cloud.rpc.enhancement.stat.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for stat reporter.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.stat")
public class PolarisStatProperties {

	/**
	 * If state reporter enabled.
	 */
	private boolean enabled = false;

	/**
	 * Path for prometheus to pull.
	 */
	private String path = "/metrics";

	/**
	 * The path regex list for stat for aggregation.
	 */
	private List<String> pathRegexList = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}


	public List<String> getPathRegexList() {
		return pathRegexList;
	}

	public void setPathRegexList(List<String> pathRegexList) {
		this.pathRegexList = pathRegexList;
	}

	@Override
	public String toString() {
		return "PolarisStatProperties{" +
				"enabled=" + enabled +
				", path='" + path + '\'' +
				", pathRegexList=" + pathRegexList +
				'}';
	}
}
