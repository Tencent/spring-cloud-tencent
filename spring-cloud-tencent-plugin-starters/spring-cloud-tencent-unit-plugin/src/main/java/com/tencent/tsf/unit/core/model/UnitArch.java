/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.model;

import java.util.List;

/**
 * 详细定义参考 iwiki /p/4009047246.
 */
public class UnitArch {

	private TencentUnitArch tencent;

	public TencentUnitArch getTencent() {
		return tencent;
	}

	public void setTencent(TencentUnitArch tencent) {
		this.tencent = tencent;
	}

	public static class TencentUnitArch {
		private UnitCloudArch unitCloudArchitecture;

		public UnitCloudArch getUnitCloudArchitecture() {
			return unitCloudArchitecture;
		}

		public void setUnitCloudArchitecture(UnitCloudArch unitCloudArchitecture) {
			this.unitCloudArchitecture = unitCloudArchitecture;
		}
	}

	public static class UnitCloudArch {

		private String localCloudId;

		private List<CloudSpace> cloudSpaces;

		private List<BusinessSystem> businessSystems;

		public String getLocalCloudId() {
			return localCloudId;
		}

		public void setLocalCloudId(String localCloudId) {
			this.localCloudId = localCloudId;
		}

		public List<CloudSpace> getCloudSpaces() {
			return cloudSpaces;
		}

		public void setCloudSpaces(List<CloudSpace> cloudSpaces) {
			this.cloudSpaces = cloudSpaces;
		}

		public List<BusinessSystem> getBusinessSystems() {
			return businessSystems;
		}

		public void setBusinessSystems(
				List<BusinessSystem> businessSystems) {
			this.businessSystems = businessSystems;
		}
	}

	public static class CloudSpace {
		private String cloudId;

		private String cloudName;

		private String regionId;

		private String RegionName;

		private List<Sdu> sdus;

		private List<Gdu> gdus;

		private List<Sdu> graySdus;

		private List<Gdu> grayGdus;

		private List<Gateway> gateways;

		private List<Gateway> scopeGateways;

		private String mappingServiceUrl;

		public String getCloudId() {
			return cloudId;
		}

		public void setCloudId(String cloudId) {
			this.cloudId = cloudId;
		}

		public String getCloudName() {
			return cloudName;
		}

		public void setCloudName(String cloudName) {
			this.cloudName = cloudName;
		}

		public String getRegionId() {
			return regionId;
		}

		public void setRegionId(String regionId) {
			this.regionId = regionId;
		}

		public String getRegionName() {
			return RegionName;
		}

		public void setRegionName(String regionName) {
			RegionName = regionName;
		}

		public List<Sdu> getSdus() {
			return sdus;
		}

		public void setSdus(List<Sdu> sdus) {
			this.sdus = sdus;
		}

		public List<Gdu> getGdus() {
			return gdus;
		}

		public void setGdus(List<Gdu> gdus) {
			this.gdus = gdus;
		}

		public List<Sdu> getGraySdus() {
			return graySdus;
		}

		public void setGraySdus(List<Sdu> graySdus) {
			this.graySdus = graySdus;
		}

		public List<Gdu> getGrayGdus() {
			return grayGdus;
		}

		public void setGrayGdus(List<Gdu> grayGdus) {
			this.grayGdus = grayGdus;
		}

		public List<Gateway> getGateways() {
			return gateways;
		}

		public void setGateways(List<Gateway> gateways) {
			this.gateways = gateways;
		}

		public List<Gateway> getScopeGateways() {
			return scopeGateways;
		}

		public void setScopeGateways(List<Gateway> scopeGateways) {
			this.scopeGateways = scopeGateways;
		}

		public String getMappingServiceUrl() {
			return mappingServiceUrl;
		}

		public void setMappingServiceUrl(String mappingServiceUrl) {
			this.mappingServiceUrl = mappingServiceUrl;
		}
	}

	public static class Sdu extends DeploymentUnit {
		private GduRule gduRule;

		public GduRule getGduRule() {
			return gduRule;
		}

		public void setGduRule(GduRule gduRule) {
			this.gduRule = gduRule;
		}
	}

	public static class Gdu extends DeploymentUnit {

	}

	public static class DeploymentUnit {
		private String id;

		private String name;

		private String zoneId;

		private String zoneName;

		private List<UnitNamespace> namespaces;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getZoneId() {
			return zoneId;
		}

		public void setZoneId(String zoneId) {
			this.zoneId = zoneId;
		}

		public String getZoneName() {
			return zoneName;
		}

		public void setZoneName(String zoneName) {
			this.zoneName = zoneName;
		}

		public List<UnitNamespace> getNamespaces() {
			return namespaces;
		}

		public void setNamespaces(List<UnitNamespace> namespaces) {
			this.namespaces = namespaces;
		}
	}

	public static class Gateway {
		private String id;

		private Address address;

		private String businessSystemName;

		private RouteScope routeScope;

		private String serviceName;

		private String namespaceId;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public String getBusinessSystemName() {
			return businessSystemName;
		}

		public void setBusinessSystemName(String businessSystemName) {
			this.businessSystemName = businessSystemName;
		}

		public RouteScope getRouteScope() {
			return routeScope;
		}

		public void setRouteScope(RouteScope routeScope) {
			this.routeScope = routeScope;
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

		@Override
		public String toString() {
			return "Gateway{" +
					"id='" + id + '\'' +
					", address=" + address +
					", businessSystemName='" + businessSystemName + '\'' +
					", serviceName='" + serviceName + '\'' +
					", namespaceId='" + namespaceId + '\'' +
					'}';
		}
	}

	public static class Address {
		private String host;

		private String port;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getPort() {
			return port;
		}

		public void setPort(String port) {
			this.port = port;
		}

		@Override
		public String toString() {
			return "Address{" +
					"host='" + host + '\'' +
					", port='" + port + '\'' +
					'}';
		}
	}

	public static class GduRule {
		private GduRuleRoute route;

		public GduRuleRoute getRoute() {
			return route;
		}

		public void setRoute(GduRuleRoute route) {
			this.route = route;
		}
	}

	public static class BusinessSystem {
		private String id;

		private String name;

		private String type;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	public static class GduRuleRoute {
		private String cloudSpaceId;

		private String unitId;

		public String getCloudSpaceId() {
			return cloudSpaceId;
		}

		public void setCloudSpaceId(String cloudSpaceId) {
			this.cloudSpaceId = cloudSpaceId;
		}

		public String getUnitId() {
			return unitId;
		}

		public void setUnitId(String unitId) {
			this.unitId = unitId;
		}
	}

	public enum RouteScope {
		/**
		 * LOCAL.
		 */
		LOCAL,
		/**
		 * REMOTE.
		 */
		REMOTE,
		/**
		 * LOCAL_REMOTE.
		 */
		LOCAL_REMOTE
	}
}
