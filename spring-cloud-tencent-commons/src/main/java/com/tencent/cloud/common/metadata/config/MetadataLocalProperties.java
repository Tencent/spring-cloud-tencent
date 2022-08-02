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
 */

package com.tencent.cloud.common.metadata.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

/**
 * Metadata Properties from local properties file.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.tencent.metadata")
public class MetadataLocalProperties {

	/**
	 * metadata content.
	 */
	private Map<String, String> content;

	/**
	 * transitive metadata key list.
	 */
	private List<String> transitive;

	/**
	 * A disposable metadata key list .
	 */
	private List<String> disposable;

	public Map<String, String> getContent() {
		if (CollectionUtils.isEmpty(content)) {
			content = new HashMap<>();
		}
		return content;
	}

	public void setContent(Map<String, String> content) {
		this.content = content;
	}

	public List<String> getTransitive() {
		if (CollectionUtils.isEmpty(transitive)) {
			transitive = new ArrayList<>();
		}
		return transitive;
	}

	public void setTransitive(List<String> transitive) {
		this.transitive = transitive;
	}

	public List<String> getDisposable() {
		if (CollectionUtils.isEmpty(disposable)) {
			disposable = new ArrayList<>();
		}
		return disposable;
	}

	public void setDisposable(List<String> disposable) {
		this.disposable = disposable;
	}
}
