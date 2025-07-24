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
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.polaris.api.utils.StringUtils;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(TencentUnitContext.class);
	// 用户标签，客户可以直接设置的
	private final static ThreadLocal<HashMap<String, String>> USER_CONTEXTS = ThreadLocal.withInitial(HashMap::new);
	// 系统标签，用于中间的计算，部分需要传递
	private final static ThreadLocal<HashMap<String, String>> SYSTEM_CONTEXTS = ThreadLocal.withInitial(HashMap::new);
	// 路由标签, key 为 CloudSpaceRoute 前缀的，提供给 TsfConsulReactiveCommonDiscoveryClient 做服务发现使用，无需传递
	private final static ThreadLocal<HashMap<String, Object>> ROUTE_CONTEXTS = ThreadLocal.withInitial(HashMap::new);
	// 上游标签，不需要放到 header 传递
	private final static ThreadLocal<HashMap<String, String>> SOURCE_CONTEXTS = ThreadLocal.withInitial(HashMap::new);
	// 用户灰度标签，用于匹配灰度规则，不需要放到 header 传递
	private final static ThreadLocal<HashMap<String, String>> GRAY_USER_CONTEXTS = ThreadLocal.withInitial(HashMap::new);
	// 灰度内部标签，不需要放到 header 传递
	private final static ThreadLocal<HashMap<String, String>> GRAY_SYSTEM_CONTEXTS = ThreadLocal.withInitial(HashMap::new);

	private TencentUnitContext() {
	}

	@Deprecated
	public static void putTag(String key, String value) {
		putUserTag(key, value);
	}

	public static void putUserTag(String key, String value) {
		USER_CONTEXTS.get().put(key, value);
	}

	public static void putSystemTag(String key, String value) {
		SYSTEM_CONTEXTS.get().put(key, value);
	}

	public static void putRouteTag(String key, Object value) {
		ROUTE_CONTEXTS.get().put(key, value);
	}

	public static void putSourceTag(String key, String value) {
		SOURCE_CONTEXTS.get().put(SOURCE_PREFIX + key, value);
	}

	public static void putGrayUserTags(String position, Map<String, String> labels) {
		for (Map.Entry<String, String> label : labels.entrySet()) {
			putGrayUserTag(position, label.getKey(), label.getValue());
		}
	}

	// 需要携带 position，做灰度路由匹配用的
	public static void putGrayUserTag(String position, String key, String value) {
		GRAY_USER_CONTEXTS.get().put(getGrayPositionPrefix(position) + key, value);
	}

	// 传递灰度单元信息用的
	public static void putGraySystemTag(String key, String value) {
		GRAY_SYSTEM_CONTEXTS.get().put(getGrayPrefix() + key, value);
	}

	public static String getGraySystemTag(String key) {
		return GRAY_SYSTEM_CONTEXTS.get().get(getGrayPrefix() + key);
	}

	public static boolean grayContextIsEmpty() {
		return GRAY_USER_CONTEXTS.get().isEmpty();
	}

	public static String getGrayPositionPrefix(String position) {
		return GRAY_PREFIX + position.toLowerCase(Locale.ROOT) + ".";
	}

	public static String getGrayPrefix() {
		return GRAY_PREFIX;
	}

	public static void putSystemTagsFromUser() {
		for (String key : CLOUD_SPACE_SYSTEM_FROM_USER_KEYS) {
			if (USER_CONTEXTS.get().containsKey(key)) {
				SYSTEM_CONTEXTS.get().put(key, USER_CONTEXTS.get().get(key));
			}
		}
	}

	public static void putSystemTags(Map<String, String> tags) {
		for (Map.Entry<String, String> tag : tags.entrySet()) {
			SYSTEM_CONTEXTS.get().put(tag.getKey(), tag.getValue());
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
		return USER_CONTEXTS.get().get(key);
	}

	public static String getSystemTag(String key) {
		return SYSTEM_CONTEXTS.get().get(key);
	}

	public static Object getObjectRouteTag(String key) {
		return ROUTE_CONTEXTS.get().get(key);
	}

	public static String getStringRouteTag(String key) {
		return (String) ROUTE_CONTEXTS.get().get(key);
	}

	public static boolean containRouteTag(String key) {
		return ROUTE_CONTEXTS.get().containsKey(key);
	}

	public static void removeAll() {
		USER_CONTEXTS.get().clear();
		SYSTEM_CONTEXTS.get().clear();
		ROUTE_CONTEXTS.get().clear();
		SOURCE_CONTEXTS.get().clear();
		GRAY_USER_CONTEXTS.get().clear();
		GRAY_SYSTEM_CONTEXTS.get().clear();
	}

	public static void clearGrayUserContext() {
		GRAY_USER_CONTEXTS.get().clear();
	}

	/**
	 * 清理客户直接设置或直接影响的 key.
	 */
	public static void clearUserTags() {
		USER_CONTEXTS.get().remove(CLOUD_SPACE_TARGET_UNIT_ID);
		USER_CONTEXTS.get().remove(CLOUD_SPACE_CUSTOMER_IDENTIFIER);
		USER_CONTEXTS.get().remove(CLOUD_SPACE_TARGET_SYSTEM);
	}

	public static void setUnitCompositeContextMap(UnitCompositeContextMap unitCompositeContextMap) {
		removeAll();

		USER_CONTEXTS.get().putAll(unitCompositeContextMap.getUserContext());
		SYSTEM_CONTEXTS.get().putAll(unitCompositeContextMap.getSystemContext());
		SOURCE_CONTEXTS.get().putAll(unitCompositeContextMap.getSourceContext());
		GRAY_USER_CONTEXTS.get().putAll(unitCompositeContextMap.getGrayUserContext());
		GRAY_SYSTEM_CONTEXTS.get().putAll(unitCompositeContextMap.getGraySystemContext());
	}


	public static UnitCompositeContextMap getCompositeContextMap() {
		return new UnitCompositeContextMap(USER_CONTEXTS.get(), SYSTEM_CONTEXTS.get(), ROUTE_CONTEXTS.get(),
				SOURCE_CONTEXTS.get(), GRAY_USER_CONTEXTS.get(), GRAY_SYSTEM_CONTEXTS.get());
	}

	public static String getSourceTag(String key) {
		return SOURCE_CONTEXTS.get().get(SOURCE_PREFIX + key);
	}

	public static String getGrayTag(String position, String key) {
		return GRAY_USER_CONTEXTS.get().get(getGrayPositionPrefix(position) + key);
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
