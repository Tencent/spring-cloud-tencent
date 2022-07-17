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

package com.tencent.cloud.plugin.gateway.staining.rule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for rule staining.
 * @author lepdou 2022-07-11
 */
@ConfigurationProperties("spring.cloud.tencent.plugin.scg.staining.rule-staining")
public class RuleStainingProperties {

	@Value("${spring.cloud.tencent.plugin.scg.staining.rule-staining.namespace:${spring.cloud.tencent.namespace:default}}")
	private String namespace;

	@Value("${spring.cloud.tencent.plugin.scg.staining.rule-staining.group:${spring.application.name:spring-cloud-gateway}}")
	private String group;

	@Value("${spring.cloud.tencent.plugin.scg.staining.rule-staining.fileName:rule/staining.json}")
	private String fileName;

	private boolean enabled = true;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
