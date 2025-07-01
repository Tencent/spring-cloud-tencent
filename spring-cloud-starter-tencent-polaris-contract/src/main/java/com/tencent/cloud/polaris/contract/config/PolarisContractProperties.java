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

package com.tencent.cloud.polaris.contract.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Polaris contract.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.contract")
public class PolarisContractProperties implements ContractProperties {

	private boolean enabled = true;
	/**
	 * Packages to be scanned. Split by ",".
	 */
	private String basePackage;
	/**
	 * Paths to be excluded. Split by ",".
	 */
	private String excludePath;
	/**
	 * Group to create swagger docket.
	 */
	private String group = "polaris";
	/**
	 * Base paths to be scanned. Split by ",".
	 */
	private String basePath = "/**";

	private boolean exposure = true;

	@Value("${spring.cloud.polaris.contract.report.enabled:true}")
	private boolean reportEnabled = true;

	private String name;

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String getBasePackage() {
		return basePackage;
	}

	@Override
	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	@Override
	public String getExcludePath() {
		return excludePath;
	}

	@Override
	public void setExcludePath(String excludePath) {
		this.excludePath = excludePath;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public void setGroup(String group) {
		this.group = group;
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public boolean isExposure() {
		return exposure;
	}

	@Override
	public void setExposure(boolean exposure) {
		this.exposure = exposure;
	}

	@Override
	public boolean isReportEnabled() {
		return reportEnabled;
	}

	@Override
	public void setReportEnabled(boolean reportEnabled) {
		this.reportEnabled = reportEnabled;
	}

	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
