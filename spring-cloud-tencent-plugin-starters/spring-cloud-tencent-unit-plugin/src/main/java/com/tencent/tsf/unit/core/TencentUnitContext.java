/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.MetadataContextUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayMatchRouteUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TencentUnitContext {

	/**
	 * 内部的 key 都以 CloudSpace 开头.
	 */
	public static final String CLOUD_SPACE_TARGET_UNIT_ID = "CloudSpaceTargetUnitId";
	/**
	 * 是否限定转发到 gdu 服务，true/false.
	 */
	public static final String CLOUD_SPACE_GDU_FORWARD_ONLY = "CloudSpaceGduFrowardOnly";
	/**
	 * 是否限定转发到 sdu 服务，true/false，内部变量，可能会调整.
	 */
	public static final String CLOUD_SPACE_SDU_FORWARD_ONLY = "CloudSpaceSduFrowardOnly";
	/**
	 * CLOUD_SPACE_GDU_INSTANCE_EXIST.
	 */
	public static final String CLOUD_SPACE_GDU_INSTANCE_EXIST = "CloudSpaceGduInstanceExist";
	/**
	 * 目标 cloud space，用于判断是否转发到下一个网关，网关时判断.
	 */
	public static final String CLOUD_SPACE_TARGET_CLOUD = "CloudSpaceTargetCloud";
	/**
	 * 目标 ns id，如果是 gdu forward only，这里的目标 ns 即 gdu ns.
	 */
	public static final String CLOUD_SPACE_TARGET_NAMESPACE_ID = "CloudSpaceTargetNamespaceId";
	/**
	 * CLOUD_SPACE_TARGET_NAMESPACE_NAME.
	 */
	public static final String CLOUD_SPACE_TARGET_NAMESPACE_NAME = "CloudSpaceTargetNamespaceName";
	/**
	 * 客户要素传递.
	 */
	public static final String CLOUD_SPACE_CUSTOMER_IDENTIFIER = "CloudSpaceCustomerIdentifier";
	/**
	 * 业务系统传递.
	 */
	public static final String CLOUD_SPACE_TARGET_SYSTEM = "CloudSpaceTargetSystem";
	/**
	 * gw转发时用, 目标 ms.
	 */
	public static final String CLOUD_SPACE_TARGET_SERVICE = "CloudSpaceTargetService";
	/**
	 * 灰度单元信息，进入了灰度单元，将 GrayMatchRouteUnit list 的 json 序列化设置进去.
	 */
	public static final String CLOUD_SPACE_GRAY_UNIT_INFO = "CloudSpaceGrayUnitInfo";
	/**
	 * GrayCloudSpaceGduUnitId.
	 */
	public static final String GRAY_CLOUD_SPACE_GDU_UNIT_ID = "GrayCloudSpaceGduUnitId";
	/**
	 * GrayCloudSpaceSduUnitId.
	 */
	public static final String GRAY_CLOUD_SPACE_SDU_UNIT_ID = "GrayCloudSpaceSduUnitId";
	/**
	 * 根据目标客户要素计算出的目标客户号.
	 */
	public static final String CLOUD_SPACE_TARGET_CUSTOMER_NUMBER = "CloudSpaceTargetCustomerNumber";
	/**
	 * 根据目标客户号计算出的ShardingKey，后续通过这个ShardingKey可以算出路由到目标单元号.
	 */
	public static final String CLOUD_SPACE_TARGET_SHARDING_KEY = "CloudSpaceTargetShardingKey";
	/**
	 * 路由上下文标签，路由网关，如果 value 不为空，则需要转发到网关.
	 */
	public static final String CLOUD_SPACE_ROUTE_GATEWAY = "CloudSpaceRouteGateway";
	/**
	 * CloudSpaceRouteTargetNamespaceId.
	 */
	public static final String CLOUD_SPACE_ROUTE_TARGET_NAMESPACE_ID = "CloudSpaceRouteTargetNamespaceId";
	/**
	 * 需要从 user context 转移到 system context 的 key.
	 */
	public static final Set<String> CLOUD_SPACE_SYSTEM_FROM_USER_KEYS = new HashSet<>(
			Arrays.asList(CLOUD_SPACE_TARGET_SYSTEM, CLOUD_SPACE_CUSTOMER_IDENTIFIER, CLOUD_SPACE_TARGET_UNIT_ID));
	/**
	 * SOURCE_PREFIX.
	 */
	public static final String SOURCE_PREFIX = "source.";
	/**
	 * GRAY_PREFIX.
	 */
	public static final String GRAY_PREFIX = "gray.";
	/**
	 * USER_CONTEXTS_KEY.
	 */
	private static final String USER_CONTEXTS_KEY = "USER_CONTEXTS";
	/**
	 * SYSTEM_CONTEXTS_KEY.
	 */
	private static final String SYSTEM_CONTEXTS_KEY = "SYSTEM_CONTEXTS";
	/**
	 * ROUTE_CONTEXTS_KEY.
	 */
	private static final String ROUTE_CONTEXTS_KEY = "ROUTE_CONTEXTS";
	/**
	 * SOURCE_CONTEXTS_KEY.
	 */
	private static final String SOURCE_CONTEXTS_KEY = "SOURCE_CONTEXTS";
	/**
	 * GRAY_USER_CONTEXTS_KEY.
	 */
	private static final String GRAY_USER_CONTEXTS_KEY = "GRAY_USER_CONTEXTS";
	/**
	 * GRAY_SYSTEM_CONTEXTS_KEY.
	 */
	private static final String GRAY_SYSTEM_CONTEXTS_KEY = "GRAY_SYSTEM_CONTEXTS";

	private static final Logger LOGGER = LoggerFactory.getLogger(TencentUnitContext.class);

	private static Map<String, String> getUserContext() {
		MetadataObjectValue<Map<String, String>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(USER_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, String> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(USER_CONTEXTS_KEY, map);
			return map;
		}
	}

	private static Map<String, String> getSystemContext() {
		MetadataObjectValue<Map<String, String>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(SYSTEM_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, String> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(SYSTEM_CONTEXTS_KEY, map);
			return map;
		}
	}

	private static Map<String, Object> getRouteContext() {
		MetadataObjectValue<Map<String, Object>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(ROUTE_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, Object> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(ROUTE_CONTEXTS_KEY, map);
			return map;
		}
	}

	private static Map<String, String> getSourceContext() {
		MetadataObjectValue<Map<String, String>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(SOURCE_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, String> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(SOURCE_CONTEXTS_KEY, map);
			return map;
		}
	}

	private static Map<String, String> getGrayUserContext() {
		MetadataObjectValue<Map<String, String>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(GRAY_USER_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, String> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(GRAY_USER_CONTEXTS_KEY, map);
			return map;
		}
	}

	private static Map<String, String> getGraySystemContext() {
		MetadataObjectValue<Map<String, String>> metadataObjectValue = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.APPLICATION, true).
				getMetadataValue(GRAY_SYSTEM_CONTEXTS_KEY);
		if (MetadataContextUtils.existMetadataValue(metadataObjectValue)) {
			return metadataObjectValue.getObjectValue().get();
		}
		else {
			Map<String, String> map = new HashMap<>();
			MetadataContextUtils.putMetadataObjectValue(GRAY_SYSTEM_CONTEXTS_KEY, map);
			return map;
		}
	}

	private TencentUnitContext() {
	}

	@Deprecated
	public static void putTag(String key, String value) {
		putUserTag(key, value);
	}

	public static void putUserTag(String key, String value) {
		getUserContext().put(key, value);
	}

	public static void putSystemTag(String key, String value) {
		getSystemContext().put(key, value);
	}

	public static void putRouteTag(String key, Object value) {
		getRouteContext().put(key, value);
	}

	public static void putSourceTag(String key, String value) {
		getSourceContext().put(SOURCE_PREFIX + key, value);
	}

	public static void putGrayUserTags(String position, Map<String, String> labels) {
		for (Map.Entry<String, String> label : labels.entrySet()) {
			putGrayUserTag(position, label.getKey(), label.getValue());
		}
	}

	// 需要携带 position，做灰度路由匹配用的
	public static void putGrayUserTag(String position, String key, String value) {
		getGrayUserContext().put(getGrayPositionPrefix(position) + key, value);
	}

	// 传递灰度单元信息用的
	public static void putGraySystemTag(String key, String value) {
		getGraySystemContext().put(getGrayPrefix() + key, value);
	}

	public static String getGraySystemTag(String key) {
		return getGraySystemContext().get(getGrayPrefix() + key);
	}

	public static boolean grayContextIsEmpty() {
		return getGrayUserContext().isEmpty();
	}

	public static String getGrayPositionPrefix(String position) {
		return GRAY_PREFIX + position.toLowerCase(Locale.ROOT) + ".";
	}

	public static String getGrayPrefix() {
		return GRAY_PREFIX;
	}

	public static void putSystemTagsFromUser() {
		for (String key : CLOUD_SPACE_SYSTEM_FROM_USER_KEYS) {
			if (getUserContext().containsKey(key)) {
				getSystemContext().put(key, getUserContext().get(key));
			}
		}
	}

	public static void putSystemTags(Map<String, String> tags) {
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			getSystemContext().put(tag.getKey(), tag.getValue());
		}
	}

	public static void putSourceTags(Map<String, String> tags) {
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			putSourceTag(tag.getKey(), tag.getValue());
		}
	}

	@Deprecated
	public static String getTag(String key) {
		return getUserTag(key);
	}

	public static String getUserTag(String key) {
		return getUserContext().get(key);
	}

	public static String getSystemTag(String key) {
		return getSystemContext().get(key);
	}

	public static Object getObjectRouteTag(String key) {
		return getRouteContext().get(key);
	}

	public static String getStringRouteTag(String key) {
		return (String) getRouteContext().get(key);
	}

	public static boolean containRouteTag(String key) {
		return getRouteContext().containsKey(key);
	}

	public static void removeAll() {
		getUserContext().clear();
		getSystemContext().clear();
		getRouteContext().clear();
		getSourceContext().clear();
		getGrayUserContext().clear();
		getGraySystemContext().clear();
	}

	public static void clearGrayUserContext() {
		getGrayUserContext().clear();
	}

	/**
	 * 清理客户直接设置或直接影响的 key.
	 */
	public static void clearUserTags() {
		getUserContext().remove(CLOUD_SPACE_TARGET_UNIT_ID);
		getUserContext().remove(CLOUD_SPACE_CUSTOMER_IDENTIFIER);
		getUserContext().remove(CLOUD_SPACE_TARGET_SYSTEM);
	}

	public static void setUnitCompositeContextMap(UnitCompositeContextMap unitCompositeContextMap) {
		removeAll();

		getUserContext().putAll(unitCompositeContextMap.getUserContext());
		getSystemContext().putAll(unitCompositeContextMap.getSystemContext());
		getSourceContext().putAll(unitCompositeContextMap.getSourceContext());
		getGrayUserContext().putAll(unitCompositeContextMap.getGrayUserContext());
		getGraySystemContext().putAll(unitCompositeContextMap.getGraySystemContext());
	}


	public static UnitCompositeContextMap getCompositeContextMap() {
		return new UnitCompositeContextMap(new HashMap<>(getUserContext()), new HashMap<>(getSystemContext()), new HashMap<>(getRouteContext()),
				new HashMap<>(getSourceContext()), new HashMap<>(getGrayUserContext()), new HashMap<>(getGraySystemContext()));
	}

	/**
	 * Internal Use.
	 */
	public static UnitCompositeContextMap getOriginCompositeContextMap() {
		return new UnitCompositeContextMap(getUserContext(), getSystemContext(), getRouteContext(),
				getSourceContext(), getGrayUserContext(), getGraySystemContext());
	}

	public static String getSourceTag(String key) {
		return getSourceContext().get(SOURCE_PREFIX + key);
	}

	public static String getGrayTag(String position, String key) {
		return getGrayUserContext().get(getGrayPositionPrefix(position) + key);
	}

	public static List<GrayMatchRouteUnit> parseGrayMatchRouteUnitList() {
		String json = getSourceTag(CLOUD_SPACE_GRAY_UNIT_INFO);
		List<GrayMatchRouteUnit> result = null;

		if (StringUtils.isNotEmpty(json)) {
			result = JacksonUtils.deserialize(json, new TypeReference<List<GrayMatchRouteUnit>>() { });
		}
		return result;
	}

	public static void setGrayUnitContext(List<GrayMatchRouteUnit> grayMatchRouteUnitList) {
		// 目前应该只有一个 gdu 和 sdu
		if (grayMatchRouteUnitList != null && grayMatchRouteUnitList.size() == 2) {
			// 需要根据 unit id + system 才能获取 ns 信息，这里设置 unit id
			putGraySystemTag(GRAY_CLOUD_SPACE_GDU_UNIT_ID, grayMatchRouteUnitList.get(0).getId());
			putGraySystemTag(GRAY_CLOUD_SPACE_SDU_UNIT_ID, grayMatchRouteUnitList.get(1).getId());

			// 以 gray 信息为准，优先级高，清空可能存在的 target unit id
			putSystemTag(CLOUD_SPACE_TARGET_UNIT_ID, null);
		}
		else {
			LOGGER.warn("[setGrayUnitContext] gray route format error:{}", grayMatchRouteUnitList);
		}

	}

	public static class UnitCompositeContextMap {
		private Map<String, String> userContext;

		private Map<String, String> systemContext;

		private Map<String, Object> routeContext;

		private Map<String, String> sourceContext;

		private Map<String, String> grayUserContext;

		private Map<String, String> graySystemContext;

		public UnitCompositeContextMap() {
			this.userContext = Collections.emptyMap();
			this.systemContext = Collections.emptyMap();
			this.routeContext = Collections.emptyMap();
			this.sourceContext = Collections.emptyMap();
			this.grayUserContext = Collections.emptyMap();
			this.graySystemContext = Collections.emptyMap();
		}

		public UnitCompositeContextMap(Map<String, String> userContext,
				Map<String, String> systemContext, Map<String, Object> routeContext, Map<String, String> sourceContext,
				Map<String, String> grayUserContext, Map<String, String> graySystemContext) {
			this.userContext = userContext;
			this.systemContext = systemContext;
			this.routeContext = routeContext;
			this.sourceContext = sourceContext;
			this.grayUserContext = grayUserContext;
			this.graySystemContext = graySystemContext;
		}

		public Map<String, String> getUserContext() {
			return userContext;
		}

		public void setUserContext(Map<String, String> userContext) {
			this.userContext = userContext;
		}

		public Map<String, String> getSystemContext() {
			return systemContext;
		}

		public void setSystemContext(Map<String, String> systemContext) {
			this.systemContext = systemContext;
		}

		public Map<String, Object> getRouteContext() {
			return routeContext;
		}

		public void setRouteContext(Map<String, Object> routeContext) {
			this.routeContext = routeContext;
		}

		public Map<String, String> getSourceContext() {
			return sourceContext;
		}

		public void setSourceContext(Map<String, String> sourceContext) {
			this.sourceContext = sourceContext;
		}

		public Map<String, String> getGrayUserContext() {
			return grayUserContext;
		}

		public void setGrayUserContext(Map<String, String> grayUserContext) {
			this.grayUserContext = grayUserContext;
		}

		public Map<String, String> getGraySystemContext() {
			return graySystemContext;
		}

		public void setGraySystemContext(Map<String, String> graySystemContext) {
			this.graySystemContext = graySystemContext;
		}

		public String getSystemTag(String key) {
			return systemContext.get(key);
		}

		public String getRouteStringTag(String key) {
			if (routeContext.containsKey(key)) {
				return String.valueOf(routeContext.get(key));
			}
			else {
				return null;
			}
		}

		public boolean containRouteTag(String key) {
			return routeContext.containsKey(key);
		}

		public Object getRouteTag(String key) {
			return routeContext.get(key);
		}

		public String getSourceTag(String key) {
			return sourceContext.get(SOURCE_PREFIX + key);
		}

		public String getGraySystemTag(String key) {
			return graySystemContext.get(GRAY_PREFIX + key);
		}

		public String getGrayTag(String position, String key) {
			return grayUserContext.get(getGrayPositionPrefix(position) + key);
		}

		public boolean isForwardToGateway() {
			return routeContext.containsKey(CLOUD_SPACE_ROUTE_GATEWAY);
		}

		public boolean containTargetNamespaceId() {
			return routeContext.containsKey(CLOUD_SPACE_ROUTE_TARGET_NAMESPACE_ID);
		}

		@Override
		public String toString() {
			return "UnitCompositeContextMap{" +
					"userContext=" + userContext +
					", systemContext=" + systemContext +
					", routeContext=" + routeContext +
					", sourceContext=" + sourceContext +
					", grayUserContext=" + grayUserContext +
					", graySystemContext=" + graySystemContext +
					'}';
		}
	}
}
