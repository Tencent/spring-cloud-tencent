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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 路径通配规则.
 * @author clarezzhang
 */
public class PathWildcardRule {

	/**
	 * 路径通配规则ID.
	 */
	private String wildCardId;

	/**
	 * 网关部署组ID.
	 */
	private String groupId;

	/**
	 * 通配路径.
	 */
	private String wildCardPath;

	/**
	 * 通配方法.
	 */
	private String method;

	/**
	 * 网关微服务ID.
	 */
	private String serviceId;

	/**
	 * 网关微服务名称.
	 */
	private String serviceName;

	/**
	 * 网关命名空间ID.
	 */
	private String namespaceId;

	/**
	 * 网关命名空间名称.
	 */
	private String namespaceName;

	/**
	 * 超时时间.
	 */
	private Integer timeout;

	/**
	 * 路径通配规则IDs.
	 */
	@JsonIgnore
	private List<String> wildCardIds;

	public String getWildCardId() {
		return wildCardId;
	}

	public void setWildCardId(String wildCardId) {
		this.wildCardId = wildCardId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespaceName() {
		return namespaceName;
	}

	public void setNamespaceName(String namespaceName) {
		this.namespaceName = namespaceName;
	}

	public String getWildCardPath() {
		return wildCardPath;
	}

	public void setWildCardPath(String wildCardPath) {
		this.wildCardPath = wildCardPath;
	}

	public List<String> getWildCardIds() {
		return wildCardIds;
	}

	public void setWildCardIds(List<String> wildCardIds) {
		this.wildCardIds = wildCardIds;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		return "PathWildcardRule{" +
				"wildCardId='" + wildCardId + '\'' +
				", groupId='" + groupId + '\'' +
				", wildCardPath='" + wildCardPath + '\'' +
				", method='" + method + '\'' +
				", serviceId='" + serviceId + '\'' +
				", serviceName='" + serviceName + '\'' +
				", namespaceId='" + namespaceId + '\'' +
				", namespaceName='" + namespaceName + '\'' +
				", wildCardIds=" + wildCardIds +
				", timeout=" + timeout +
				'}';
	}
}
