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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.tsf.unit.core.UnitTag;
import com.tencent.tsf.unit.core.algorithm.IUnitTransformAlgorithm;

public class UnitRouteInfo {

	private TencentUnitRouteRule tencent;

	public TencentUnitRouteRule getTencent() {
		return tencent;
	}

	public void setTencent(TencentUnitRouteRule tencent) {
		this.tencent = tencent;
	}

	public static class TencentUnitRouteRule {
		private UnitRouteRule unitRouteRule;

		private List<UnitRoute> gduRouteRule;

		private GrayUnitRouteRule grayUnitRouteRule;

		private RouterIdentifier routerIdentifier;

		public UnitRouteRule getUnitRouteRule() {
			return unitRouteRule;
		}

		public void setUnitRouteRule(UnitRouteRule unitRouteRule) {
			this.unitRouteRule = unitRouteRule;
		}

		public List<UnitRoute> getGduRouteRule() {
			return gduRouteRule;
		}

		public void setGduRouteRule(List<UnitRoute> gduRouteRule) {
			this.gduRouteRule = gduRouteRule;
		}

		public GrayUnitRouteRule getGrayUnitRouteRule() {
			return grayUnitRouteRule;
		}

		public void setGrayUnitRouteRule(
				GrayUnitRouteRule grayUnitRouteRule) {
			this.grayUnitRouteRule = grayUnitRouteRule;
		}

		public RouterIdentifier getRouterIdentifier() {
			return routerIdentifier;
		}

		public void setRouterIdentifier(
				RouterIdentifier routerIdentifier) {
			this.routerIdentifier = routerIdentifier;
		}
	}

	public static class GrayUnitRouteRule {

		private List<GrayUnitRoute> unitRoutes;

		public List<GrayUnitRoute> getUnitRoutes() {
			return unitRoutes;
		}

		public void setUnitRoutes(
				List<GrayUnitRoute> unitRoutes) {
			this.unitRoutes = unitRoutes;
		}
	}

	public static class UnitRouteRule {
		private String id;

		private String name;

		private String desc;

		private Boolean passingThroughEnabled;

		private List<TagTransform> tagTransforms;

		private List<UnitRoute> unitRoutes;

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

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}

		public Boolean getPassingThroughEnabled() {
			return passingThroughEnabled;
		}

		public void setPassingThroughEnabled(Boolean passingThroughEnabled) {
			this.passingThroughEnabled = passingThroughEnabled;
		}

		public List<TagTransform> getTagTransforms() {
			return tagTransforms;
		}

		public void setTagTransforms(
				List<TagTransform> tagTransforms) {
			this.tagTransforms = tagTransforms;
		}

		public List<UnitRoute> getUnitRoutes() {
			return unitRoutes;
		}

		public void setUnitRoutes(List<UnitRoute> unitRoutes) {
			this.unitRoutes = unitRoutes;
		}
	}

	public static class TagTransform {
		private TagTransformLocation source;

		private TransformAction transform;

		private TagTransformLocation destination;

		public TagTransformLocation getSource() {
			return source;
		}

		public void setSource(TagTransformLocation source) {
			this.source = source;
		}

		public TransformAction getTransform() {
			return transform;
		}

		public void setTransform(TransformAction transform) {
			this.transform = transform;
		}

		public TagTransformLocation getDestination() {
			return destination;
		}

		public void setDestination(TagTransformLocation destination) {
			this.destination = destination;
		}
	}

	public static class TagTransformLocation {
		private String tagName;

		public String getTagName() {
			return tagName;
		}

		public void setTagName(String tagName) {
			this.tagName = tagName;
		}
	}

	public static class TransformAction {
		private TransformAlgorithm algorithm;
		// 解析 algorithm 后放入的实际操作类
		@JsonIgnore
		private IUnitTransformAlgorithm unitTransformAlgorithm;

		public TransformAlgorithm getAlgorithm() {
			return algorithm;
		}

		public void setAlgorithm(TransformAlgorithm algorithm) {
			this.algorithm = algorithm;
		}

		public IUnitTransformAlgorithm getUnitTransformAlgorithm() {
			return unitTransformAlgorithm;
		}

		public void setUnitTransformAlgorithm(
				IUnitTransformAlgorithm unitTransformAlgorithm) {
			this.unitTransformAlgorithm = unitTransformAlgorithm;
		}
	}

	public static class TransformAlgorithm {
		private String name;
		// algorithm 的操作算子，由 name 决定
		private Map<String, Object> options;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, Object> getOptions() {
			return options;
		}

		public void setOptions(Map<String, Object> options) {
			this.options = options;
		}
	}

	public static class GrayUnitRoute {
		private GrayUnitRouteMatch match;

		private GrayMatchRoute route;

		public GrayUnitRouteMatch getMatch() {
			return match;
		}

		public void setMatch(GrayUnitRouteMatch match) {
			this.match = match;
		}

		public GrayMatchRoute getRoute() {
			return route;
		}

		public void setRoute(GrayMatchRoute route) {
			this.route = route;
		}
	}

	public static class UnitRoute {
		private UnitRouteMatch match;

		private MatchRoute route;

		public UnitRouteMatch getMatch() {
			return match;
		}

		public void setMatch(UnitRouteMatch match) {
			this.match = match;
		}

		public MatchRoute getRoute() {
			return route;
		}

		public void setRoute(MatchRoute route) {
			this.route = route;
		}
	}

	public static class GrayUnitRouteMatch extends UnitRouteMatch {
		private Boolean grayListEnabled;

		public Boolean getGrayListEnabled() {
			return grayListEnabled;
		}

		public void setGrayListEnabled(Boolean grayListEnabled) {
			this.grayListEnabled = grayListEnabled;
		}
	}

	public static class UnitRouteMatch {
		private String name;

		private String operator;

		private List<UnitTag> conditions;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOperator() {
			return operator;
		}

		public void setOperator(String operator) {
			this.operator = operator;
		}

		public List<UnitTag> getConditions() {
			return conditions;
		}

		public void setConditions(List<UnitTag> conditions) {
			this.conditions = conditions;
		}
	}

	public static class GrayMatchRoute {

		private List<GrayMatchRouteUnit> units;

		public List<GrayMatchRouteUnit> getUnits() {
			return units;
		}

		public void setUnits(
				List<GrayMatchRouteUnit> units) {
			this.units = units;
		}
	}

	public static class GrayMatchRouteUnit {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "GrayMatchRouteUnit{" +
					"id='" + id + '\'' +
					'}';
		}
	}

	public static class MatchRoute {
		private String unitId;

		private MatchRouteFailover failover;

		public String getUnitId() {
			return unitId;
		}

		public void setUnitId(String unitId) {
			this.unitId = unitId;
		}

		public MatchRouteFailover getFailover() {
			return failover;
		}

		public void setFailover(MatchRouteFailover failover) {
			this.failover = failover;
		}

		public String getActualUnitId() {
			if (failover != null && failover.getEnabled()) {
				return failover.getRoute().getUnitId();
			}
			else {
				return unitId;
			}
		}
	}

	public static class MatchRouteFailover {
		private Boolean enabled;

		private FailoverRoute route;

		private String scope;

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public FailoverRoute getRoute() {
			return route;
		}

		public void setRoute(FailoverRoute route) {
			this.route = route;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}
	}

	public static class FailoverRoute {
		private String unitId;

		private FailoverRouteLocationFilter locationFilter;

		public String getUnitId() {
			return unitId;
		}

		public void setUnitId(String unitId) {
			this.unitId = unitId;
		}

		public FailoverRouteLocationFilter getLocationFilter() {
			return locationFilter;
		}

		public void setLocationFilter(FailoverRouteLocationFilter locationFilter) {
			this.locationFilter = locationFilter;
		}
	}

	public static class FailoverRouteLocationFilter {
		private UnitRouteMatch zoneFilter;

		public UnitRouteMatch getZoneFilter() {
			return zoneFilter;
		}

		public void setZoneFilter(UnitRouteMatch zoneFilter) {
			this.zoneFilter = zoneFilter;
		}
	}

	public static class RouterIdentifier {
		private String keySeparator;

		private String keyValueSeparator;

		private String shardingIdentifierKey;

		private String routerIdentifierHeader;

		private List<MappingService> shardingIdentifierMappingServices;

		@JsonIgnore
		private Map<String, MappingService> mappingServiceMap;

		public RouterIdentifier() {
		}

		public RouterIdentifier(String keySeparator, String keyValueSeparator,
				String shardingIdentifierKey,
				String routerIdentifierHeader) {
			this.keySeparator = keySeparator;
			this.keyValueSeparator = keyValueSeparator;
			this.shardingIdentifierKey = shardingIdentifierKey;
			this.routerIdentifierHeader = routerIdentifierHeader;
		}

		public String getKeySeparator() {
			return keySeparator;
		}

		public void setKeySeparator(String keySeparator) {
			this.keySeparator = keySeparator;
		}

		public String getKeyValueSeparator() {
			return keyValueSeparator;
		}

		public void setKeyValueSeparator(String keyValueSeparator) {
			this.keyValueSeparator = keyValueSeparator;
		}

		public String getShardingIdentifierKey() {
			return shardingIdentifierKey;
		}

		public void setShardingIdentifierKey(String shardingIdentifierKey) {
			this.shardingIdentifierKey = shardingIdentifierKey;
		}

		public String getRouterIdentifierHeader() {
			return routerIdentifierHeader;
		}

		public void setRouterIdentifierHeader(String routerIdentifierHeader) {
			this.routerIdentifierHeader = routerIdentifierHeader;
		}

		public List<MappingService> getShardingIdentifierMappingServices() {
			return shardingIdentifierMappingServices;
		}

		public void setShardingIdentifierMappingServices(
				List<MappingService> shardingIdentifierMappingServices) {
			this.shardingIdentifierMappingServices = shardingIdentifierMappingServices;
		}

		public Map<String, MappingService> getMappingServiceMap() {
			return mappingServiceMap;
		}

		public void setMappingServiceMap(
				Map<String, MappingService> mappingServiceMap) {
			this.mappingServiceMap = mappingServiceMap;
		}
	}

	public static class MappingService {
		private String cloudId;

		private String apiPath;

		public String getCloudId() {
			return cloudId;
		}

		public void setCloudId(String cloudId) {
			this.cloudId = cloudId;
		}

		public String getApiPath() {
			return apiPath;
		}

		public void setApiPath(String apiPath) {
			this.apiPath = apiPath;
		}
	}
}
