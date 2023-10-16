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

package com.tencent.cloud.polaris.config.configdata;

import java.util.Objects;

import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.config.PolarisCryptoConfigProperties;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.Profiles;

/**
 * A polaris configData resource from which {@link ConfigData} can be loaded.
 *
 * @author wlx
 */
public class PolarisConfigDataResource extends ConfigDataResource {

	private final PolarisConfigProperties polarisConfigProperties;

	private final PolarisCryptoConfigProperties polarisCryptoConfigProperties;

	private final PolarisContextProperties polarisContextProperties;

	private final Profiles profiles;

	private final boolean optional;

	private final String fileName;

	private final String groupName;

	private final String serviceName;

	public PolarisConfigDataResource(PolarisConfigProperties polarisConfigProperties,
			PolarisCryptoConfigProperties polarisCryptoConfigProperties,
			PolarisContextProperties polarisContextProperties,
			Profiles profiles, boolean optional,
			String fileName, String groupName, String serviceName) {
		this.polarisConfigProperties = polarisConfigProperties;
		this.polarisCryptoConfigProperties = polarisCryptoConfigProperties;
		this.polarisContextProperties = polarisContextProperties;
		this.profiles = profiles;
		this.optional = optional;
		this.fileName = fileName;
		this.groupName = groupName;
		this.serviceName = serviceName;
	}

	public PolarisConfigProperties getPolarisConfigProperties() {
		return polarisConfigProperties;
	}

	public PolarisCryptoConfigProperties getPolarisCryptoConfigProperties() {
		return polarisCryptoConfigProperties;
	}

	public PolarisContextProperties getPolarisContextProperties() {
		return polarisContextProperties;
	}

	public Profiles getProfiles() {
		return profiles;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getFileName() {
		return fileName;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getServiceName() {
		return serviceName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PolarisConfigDataResource that = (PolarisConfigDataResource) o;
		return optional == that.optional &&
				polarisConfigProperties.equals(that.polarisConfigProperties) &&
				polarisCryptoConfigProperties.equals(that.polarisCryptoConfigProperties) &&
				polarisContextProperties.equals(that.polarisContextProperties) &&
				profiles.equals(that.profiles) &&
				fileName.equals(that.fileName) &&
				groupName.equals(that.groupName) &&
				serviceName.equals(that.serviceName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polarisConfigProperties, polarisCryptoConfigProperties, polarisContextProperties, profiles,
				optional, fileName, groupName, serviceName);
	}
}
