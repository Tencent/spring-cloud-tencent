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
