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
package com.tencent.tsf.gateway.core.model;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import com.tencent.cloud.plugin.gateway.context.Position;

/**
 * @author kysonli
 * 2019/4/10 12:23
 */
public class Group implements Serializable {
	private static final long serialVersionUID = -7714152839551413735L;

	private String groupId;

	private String groupName;

	private String groupContext;

	private String releaseStatus;

	private String authMode;

	private String groupType;

	private List<GroupSecret> secretList;

	/**
	 * 命名空间参数key值.
	 */
	private String namespaceNameKey;

	/**
	 * 微服务名参数key值.
	 */
	private String serviceNameKey;

	/**
	 * 命名空间参数位置，Path，Header或Query，默认是Path.
	 */
	private String namespaceNameKeyPosition = Position.PATH.name().toLowerCase(Locale.ROOT);

	/**
	 * 微服务名参数位置，Path，Header或Query，默认是Path.
	 */
	private String serviceNameKeyPosition = Position.PATH.name().toLowerCase(Locale.ROOT);


	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupContext() {
		return groupContext;
	}

	public void setGroupContext(String groupContext) {
		this.groupContext = groupContext;
	}

	public String getReleaseStatus() {
		return releaseStatus;
	}

	public void setReleaseStatus(String releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	public String getAuthMode() {
		return authMode;
	}

	public void setAuthMode(String authMode) {
		this.authMode = authMode;
	}

	public String getGroupType() {
		return groupType;
	}

	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}

	public List<GroupSecret> getSecretList() {
		return secretList;
	}

	public void setSecretList(List<GroupSecret> secretList) {
		this.secretList = secretList;
	}

	public String getNamespaceNameKey() {
		return namespaceNameKey;
	}

	public void setNamespaceNameKey(String namespaceNameKey) {
		this.namespaceNameKey = namespaceNameKey;
	}

	public String getServiceNameKey() {
		return serviceNameKey;
	}

	public void setServiceNameKey(String serviceNameKey) {
		this.serviceNameKey = serviceNameKey;
	}

	public String getNamespaceNameKeyPosition() {
		return namespaceNameKeyPosition;
	}

	public void setNamespaceNameKeyPosition(String namespaceNameKeyPosition) {
		this.namespaceNameKeyPosition = namespaceNameKeyPosition;
	}

	public String getServiceNameKeyPosition() {
		return serviceNameKeyPosition;
	}

	public void setServiceNameKeyPosition(String serviceNameKeyPosition) {
		this.serviceNameKeyPosition = serviceNameKeyPosition;
	}

	@Override
	public String toString() {
		return "Group{" +
				"groupId='" + groupId + '\'' +
				", groupName='" + groupName + '\'' +
				", groupContext='" + groupContext + '\'' +
				", releaseStatus='" + releaseStatus + '\'' +
				", authMode='" + authMode + '\'' +
				", groupType='" + groupType + '\'' +
				", secretList=" + secretList +
				", namespaceNameKey='" + namespaceNameKey + '\'' +
				", serviceNameKey='" + serviceNameKey + '\'' +
				", namespaceNameKeyPosition='" + namespaceNameKeyPosition + '\'' +
				", serviceNameKeyPosition='" + serviceNameKeyPosition + '\'' +
				'}';
	}
}
