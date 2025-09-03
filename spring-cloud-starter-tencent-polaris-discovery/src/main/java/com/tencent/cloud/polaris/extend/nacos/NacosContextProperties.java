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

package com.tencent.cloud.polaris.extend.nacos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static shade.polaris.com.alibaba.nacos.api.common.Constants.DEFAULT_CLUSTER_NAME;
import static shade.polaris.com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static shade.polaris.com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;

/**
 * Discovery configuration of Nacos.
 *
 * @author lingxiao.wlx
 */
@ConfigurationProperties("spring.cloud.nacos")
public class NacosContextProperties {

	@Value("${spring.cloud.nacos.discovery.enabled:#{'false'}}")
	private boolean discoveryEnabled;

	/**
	 * if you just want to subscribe on nacos , but don't want to register your service, set it to
	 * false.
	 */
	@Value("${spring.cloud.nacos.discovery.register-enabled:#{'true'}}")
	private boolean registerEnabled;

	/**
	 * nacos discovery server address.
	 */
	@Value("${spring.cloud.nacos.discovery.server-addr:}")
	private String serverAddr;
	/**
	 * nacos discovery server address.
	 */
	@Value("${spring.cloud.nacos.discovery.ephemeral:true}")
	private boolean ephemeral;

	/**
	 * the nacos authentication username.
	 */
	@Value("${spring.cloud.nacos.discovery.username:}")
	private String username;


	/**
	 * service name to registry.
	 */
	@Value("${spring.cloud.nacos.discovery.service:${spring.application.name:}}")
	private String serviceName;

	/**
	 * the nacos authentication password.
	 */
	@Value("${spring.cloud.nacos.discovery.password:}")
	private String password;

	/**
	 * cluster name for nacos .
	 */
	@Value("${spring.cloud.nacos.discovery.cluster-name:DEFAULT}")
	private String clusterName = DEFAULT_CLUSTER_NAME;

	/**
	 * group name for nacos.
	 */
	@Value("${spring.cloud.nacos.discovery.group:DEFAULT_GROUP}")
	private String group = DEFAULT_GROUP;

	@Value("${spring.cloud.nacos.discovery.namespace:public}")
	private String namespace = DEFAULT_NAMESPACE_ID;

	@Value("${spring.cloud.nacos.discovery.weight:1.0}")
	private double weight = 1.0;

	private String contextPath;


	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public void setRegisterEnabled(boolean registerEnabled) {
		this.registerEnabled = registerEnabled;
	}

	public boolean isDiscoveryEnabled() {
		return discoveryEnabled;
	}

	public void setDiscoveryEnabled(boolean discoveryEnabled) {
		this.discoveryEnabled = discoveryEnabled;
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public void setEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
	}
	String getServiceName() {
		return serviceName;
	}

	void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	double getWeight() {
		return weight;
	}

	void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "NacosContextProperties{" +
				"discoveryEnabled=" + discoveryEnabled +
				", registerEnabled=" + registerEnabled +
				", serverAddr='" + serverAddr + '\'' +
				", ephemeral=" + ephemeral +
				", username='" + username + '\'' +
				", serviceName='" + serviceName + '\'' +
				", password='" + password + '\'' +
				", clusterName='" + clusterName + '\'' +
				", group='" + group + '\'' +
				", namespace='" + namespace + '\'' +
				", weight=" + weight +
				", contextPath='" + contextPath + '\'' +
				'}';
	}
}
