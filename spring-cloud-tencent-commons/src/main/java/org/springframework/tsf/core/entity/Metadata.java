/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package org.springframework.tsf.core.entity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.util.StringUtils;


public class Metadata implements Serializable {

	@JsonProperty("ai")
	private String applicationId = "";

	@JsonProperty("av")
	private String applicationVersion = "";

	@JsonProperty("sn")
	private String serviceName = "";

	@JsonProperty("ii")
	private String instanceId = "";

	@JsonProperty("gi")
	private String groupId = "";

	@JsonProperty("li")
	private String localIp = "";

	@JsonProperty("lis")
	private String localIps = "";

	@JsonProperty("ni")
	private String namespaceId = "";

	@JsonProperty("pi")
	private boolean preferIpv6;

	public Metadata() {
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	// 其实是程序包的版本，但是这里程序包概念没啥用，直接用应用来表示
	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getLocalIp() {
		if (preferIpv6 && !StringUtils.isEmpty(localIps)) {
			for (String ip : localIps.split(",")) {
				if (ip.contains(":")) {
					return ip;
				}
			}
		}
		return localIp;
	}

	public void setLocalIp(String localIp) {
		this.localIp = localIp;
	}

	public String getLocalIps() {
		return localIps;
	}

	public void setLocalIps(String localIps) {
		this.localIps = localIps;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public boolean isPreferIpv6() {
		return preferIpv6;
	}

	public void setPreferIpv6(boolean preferIpv6) {
		this.preferIpv6 = preferIpv6;
	}

	@Override
	public String toString() {
		return "Metadata{" +
				"applicationId='" + applicationId + '\'' +
				", applicationVersion='" + applicationVersion + '\'' +
				", serviceName='" + serviceName + '\'' +
				", instanceId='" + instanceId + '\'' +
				", groupId='" + groupId + '\'' +
				", localIp='" + localIp + '\'' +
				", namespaceId='" + namespaceId + '\'' +
				'}';
	}

}
