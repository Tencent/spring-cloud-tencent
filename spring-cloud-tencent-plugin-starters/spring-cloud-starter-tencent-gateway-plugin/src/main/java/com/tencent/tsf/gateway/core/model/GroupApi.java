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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author kysonli
 * 2019/4/10 12:20
 */
public class GroupApi implements Serializable {
	private static final long serialVersionUID = 5772774534522075805L;

	private String apiId;

	private String groupId;

	private String path;

	private String method;

	private String serviceName;

	private String namespaceId;

	private String namespaceName;

	private String releaseStatus;

	private String usableStatus;

	private String pathMapping;

	private Integer timeout;


	/**
	 * api所在服务host,格式: http://localhost:8080.
	 */
	private String host;

	/**
	 * 描述信息.
	 */
	private String description;

	/**
	 * API类型， ms ： 微服务API； external :外部服务Api.
	 */
	private String apiType;

	/**
	 * RPC 类型，http（spring cloud）, dubbo ...
	 */
	private String rpcType;

	/**
	 * RPC 额外信息，json 字符串，根据不同的 rpcType 进行解析.
	 */
	private String rpcExt;


	/**
	 * rpcExt反序列化对应的对象.
	 */
	@JsonIgnore
	private Object rpcExtObj;

	public String getApiId() {
		return apiId;
	}

	public void setApiId(String apiId) {
		this.apiId = apiId;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespaceId() {
		return namespaceId;
	}

	public void setNamespaceId(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	public String getNamespaceName() {
		return namespaceName;
	}

	public void setNamespaceName(String namespaceName) {
		this.namespaceName = namespaceName;
	}

	public String getReleaseStatus() {
		return releaseStatus;
	}

	public void setReleaseStatus(String releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	public String getUsableStatus() {
		return usableStatus;
	}

	public void setUsableStatus(String usableStatus) {
		this.usableStatus = usableStatus;
	}

	public String getPathMapping() {
		return pathMapping;
	}

	public void setPathMapping(String pathMapping) {
		this.pathMapping = pathMapping;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getApiType() {
		return apiType;
	}

	public void setApiType(String apiType) {
		this.apiType = apiType;
	}

	public String getRpcType() {
		return rpcType;
	}

	public void setRpcType(String rpcType) {
		this.rpcType = rpcType;
	}

	public String getRpcExt() {
		return rpcExt;
	}

	public void setRpcExt(String rpcExt) {
		this.rpcExt = rpcExt;
	}

	public Object getRpcExtObj() {
		return rpcExtObj;
	}

	public void setRpcExtObj(Object rpcExtObj) {
		this.rpcExtObj = rpcExtObj;
	}
}
