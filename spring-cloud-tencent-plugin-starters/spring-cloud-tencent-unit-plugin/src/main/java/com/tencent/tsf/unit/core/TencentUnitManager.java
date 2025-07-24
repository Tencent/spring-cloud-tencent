/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.algorithm.HashCodeAlgorithm;
import com.tencent.tsf.unit.core.algorithm.IUnitTransformAlgorithm;
import com.tencent.tsf.unit.core.algorithm.ModAlgorithm;
import com.tencent.tsf.unit.core.algorithm.SubstrAlgorithm;
import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;
import com.tencent.tsf.unit.core.mapping.api.MappingEntity;
import com.tencent.tsf.unit.core.model.RoutingUnit;
import com.tencent.tsf.unit.core.model.UnitArch;
import com.tencent.tsf.unit.core.model.UnitArch.CloudSpace;
import com.tencent.tsf.unit.core.model.UnitArch.DeploymentUnit;
import com.tencent.tsf.unit.core.model.UnitArch.Gateway;
import com.tencent.tsf.unit.core.model.UnitArch.Gdu;
import com.tencent.tsf.unit.core.model.UnitArch.Sdu;
import com.tencent.tsf.unit.core.model.UnitArch.TencentUnitArch;
import com.tencent.tsf.unit.core.model.UnitArch.UnitCloudArch;
import com.tencent.tsf.unit.core.model.UnitGray;
import com.tencent.tsf.unit.core.model.UnitGray.TencentUnitGray;
import com.tencent.tsf.unit.core.model.UnitGray.UnitGrayList;
import com.tencent.tsf.unit.core.model.UnitNamespace;
import com.tencent.tsf.unit.core.model.UnitRouteInfo;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayUnitRoute;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayUnitRouteRule;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.MappingService;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.MatchRoute;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.MatchRouteFailover;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.RouterIdentifier;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.TagTransform;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.TencentUnitRouteRule;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.TransformAction;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.UnitRoute;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.UnitRouteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TencentUnitManager {

	/**
	 * DEFAULT_KEY_SEPARATOR.
	 */
	public static final String DEFAULT_KEY_SEPARATOR = ",";
	/**
	 * DEFAULT_KEY_VALUE_SEPARATOR.
	 */
	public static final String DEFAULT_KEY_VALUE_SEPARATOR = "=";
	/**
	 * DEFAULT_SHARDING_IDENTIFIER_KEY.
	 */
	public static final String DEFAULT_SHARDING_IDENTIFIER_KEY = "CustomerNumber";
	/**
	 * DEFAULT_ROUTER_IDENTIFIER_HEADER.
	 */
	public static final String DEFAULT_ROUTER_IDENTIFIER_HEADER = "Router-Identifier";
	private static final Logger LOGGER = LoggerFactory.getLogger(TencentUnitManager.class);
	private static final List<IUnitChangeCallback> archCallbacks = new ArrayList<>();
	private static final ReadWriteLock archCallbackRwLock = new ReentrantReadWriteLock();
	private static final Lock archCallbackRLock = archCallbackRwLock.readLock();
	private static final Lock archCallbackWLock = archCallbackRwLock.writeLock();
	private static final List<IUnitChangeCallback> ruleCallbacks = new ArrayList<>();
	private static final ReadWriteLock ruleCallbackRwLock = new ReentrantReadWriteLock();
	private static final Lock ruleCallbackRLock = ruleCallbackRwLock.readLock();
	private static final Lock ruleCallbackWLock = ruleCallbackRwLock.writeLock();
	private volatile static UnitArch unitArch;
	private volatile static UnitRouteInfo unitRouteInfo;
	// 灰名单列表
	private volatile static Set<String> unitGrayIds = new HashSet<>();
	private volatile static List<GrayUnitRoute> grayUnitRoutes;
	private volatile static Set<String> grayUnitHeaderKey = new HashSet<>();
	private volatile static String localCloudSpaceId = "";
	private volatile static String localUnitId = null;
	private volatile static String localGduUnitId = null;
	private volatile static String localFailoverGduUnitId = null;
	// all cloud space
	private volatile static List<CloudSpace> allCloudSpaces = new ArrayList<>();
	// gdu unit id -> Gdu
	private volatile static Map<String, Gdu> allGduMap = new ConcurrentHashMap<>();
	// gray unit id -> du(gdu/sdu)
	private volatile static Map<String, DeploymentUnit> grayUnitDuMap = new ConcurrentHashMap<>();
	// gdu unit id -> { system -> ns }
	private volatile static Map<String, Map<String, UnitNamespace>> gduNsMap = new ConcurrentHashMap<>();
	// gray gdu unit id -> { system -> ns }
	private volatile static Map<String, Map<String, UnitNamespace>> grayGduNsMap = new HashMap<>();
	// (cloud id + ns id) -> unit id
	private volatile static Map<String, String> nsUnitMap = new ConcurrentHashMap<>();
	// unit id -> cloud id
	private volatile static Map<String, String> unitCloudMap = new ConcurrentHashMap<>();
	// (sdu unit id + business system name) -> gdu unit id
	private volatile static Map<String, String> businessSystemGduMap = new ConcurrentHashMap<>();
	// (gdu/sdu unit id + business system name) -> ns
	private volatile static Map<String, UnitNamespace> businessSystemNsMap = new ConcurrentHashMap<>();
	// cloud id -> { business system name -> gw}
	private volatile static Map<String, Map<String, Gateway>> businessSystemGwMap = new ConcurrentHashMap<>();
	// cloud id -> { gw id  -> local only gw}
	private volatile static Map<String, Map<String, Gateway>> localOnlyGwMap = new ConcurrentHashMap<>();
	// (gdu/sdu unit id) -> failover gdu/sdu unit id
	private volatile static Map<String, String> failoverUnitIdMap = new ConcurrentHashMap<>();
	private volatile static Map<String, Boolean> activeSduIdMap = new ConcurrentHashMap<>();
	private volatile static Set<String> disableZoneSet = new HashSet<>();
	private volatile static Set<String> businessSystemSet = new HashSet<>();
	// 一期只有一个
	private volatile static TagTransform tagTransform = null;
	private volatile static List<UnitRoute> unitRoutes = Collections.emptyList();
	private volatile static CloudSpace localCloudSpace;
	private volatile static boolean enable = false;
	// 是否透传 unit context，默认 false
	private volatile static boolean contextPassingThroughEnabled = false;
	private volatile static RouterIdentifier routerIdentifier = new RouterIdentifier(
			DEFAULT_KEY_SEPARATOR, DEFAULT_KEY_VALUE_SEPARATOR, DEFAULT_SHARDING_IDENTIFIER_KEY, DEFAULT_ROUTER_IDENTIFIER_HEADER);
	/**
	 * 未来扩展自定义转换算法.
	 */
	private volatile static IUnitTransformAlgorithm customAlgorithm;

	private TencentUnitManager() {
	}

	public static UnitArch getUnitArch() {
		return unitArch;
	}

	public static void setUnitArch(UnitArch unitArch) {

		if (!checkUnitArch(unitArch)) {
			throw new TencentUnitException(ErrorCode.LOAD_ERROR, "parse unit arch exception");
		}

		TencentUnitManager.unitArch = unitArch;

		Optional.ofNullable(unitArch).map(UnitArch::getTencent).map(TencentUnitArch::getUnitCloudArchitecture).
				map(UnitCloudArch::getLocalCloudId).ifPresent(id -> localCloudSpaceId = id);

		Optional<List<CloudSpace>> optionalCloudSpaces = Optional.ofNullable(unitArch).
				map(UnitArch::getTencent).map(TencentUnitArch::getUnitCloudArchitecture).
				map(UnitCloudArch::getCloudSpaces);

		optionalCloudSpaces.ifPresent(cloudSpaces -> {
			Map<String, String> newNsUnitMap = new ConcurrentHashMap<>();
			Map<String, String> newUnitCloudMap = new ConcurrentHashMap<>();
			Map<String, String> newBusinessSystemGduMap = new ConcurrentHashMap<>();
			Map<String, UnitNamespace> newBusinessSystemNsMap = new ConcurrentHashMap<>();
			Map<String, Map<String, Gateway>> newBusinessSystemGwMap = new ConcurrentHashMap<>();
			Map<String, Map<String, Gateway>> newLocalOnlyGwMap = new ConcurrentHashMap<>();
			Map<String, Map<String, UnitNamespace>> newGrayGduNsMap = new ConcurrentHashMap<>();
			Map<String, Map<String, UnitNamespace>> newGduNsMap = new ConcurrentHashMap<>();
			Map<String, DeploymentUnit> newGrayUnitDuMap = new ConcurrentHashMap<>();
			Map<String, Gdu> newAllGduMap = new ConcurrentHashMap<>();
			for (CloudSpace cloudSpace : cloudSpaces) {
				String gduUnitId = "";
				String grayGduUnitId = "";
				// 目前一套 cloud space 下应该最多只有一个 gdu
				if (cloudSpace.getGdus() != null) {
					for (Gdu gdu : cloudSpace.getGdus()) {
						gduUnitId = gdu.getId();
						newAllGduMap.put(gdu.getId(), gdu);
						Map<String, UnitNamespace> tmpSystemNsMap = new ConcurrentHashMap<>();
						if (StringUtils.equals(localCloudSpaceId, cloudSpace.getCloudId())) {
							for (UnitNamespace namespace : gdu.getNamespaces()) {
								newNsUnitMap.put(getCloudSpaceNamespaceId(cloudSpace.getCloudId(),
										namespace.getId()), gdu.getId());
								tmpSystemNsMap.put(namespace.getBusinessSystemName(), namespace);
								newBusinessSystemNsMap.put(getUnitBusinessSystem(
										gdu.getId(), namespace.getBusinessSystemName()), namespace);
							}
						}
						newUnitCloudMap.put(gdu.getId(), cloudSpace.getCloudId());
						if (cloudSpace.getCloudId().equals(localCloudSpaceId)) {
							localGduUnitId = gduUnitId;
						}
						newGduNsMap.put(gdu.getId(), tmpSystemNsMap);
					}
				}
				if (cloudSpace.getSdus() != null) {
					for (Sdu sdu : cloudSpace.getSdus()) {
						if (StringUtils.equals(localCloudSpaceId, cloudSpace.getCloudId())) {
							for (UnitNamespace namespace : sdu.getNamespaces()) {
								newNsUnitMap.put(getCloudSpaceNamespaceId(cloudSpace.getCloudId(),
										namespace.getId()), sdu.getId());
								// 如果 cloud space 下没有 gdu，对应 gduUnitId 为默认值空字符串
								newBusinessSystemGduMap.put(getUnitBusinessSystem(
										sdu.getId(), namespace.getBusinessSystemName()), gduUnitId);
								newBusinessSystemNsMap.put(getUnitBusinessSystem(
										sdu.getId(), namespace.getBusinessSystemName()), namespace);
							}
						}
						newUnitCloudMap.put(sdu.getId(), cloudSpace.getCloudId());
					}
				}

				if (cloudSpace.getGrayGdus() != null) {
					for (Gdu grayGdu : cloudSpace.getGrayGdus()) {
						// 未来如果有多个 gray gdu，则下面处理 gray sdu 时需要额外处理
						grayGduUnitId = grayGdu.getId();
						newGrayUnitDuMap.put(grayGduUnitId, grayGdu);
						Map<String, UnitNamespace> tmpSystemNsMap = new ConcurrentHashMap<>();
						if (StringUtils.equals(localCloudSpaceId, cloudSpace.getCloudId())) {
							for (UnitNamespace namespace : grayGdu.getNamespaces()) {
								newNsUnitMap.put(getCloudSpaceNamespaceId(cloudSpace.getCloudId(),
										namespace.getId()), grayGdu.getId());
								tmpSystemNsMap.put(namespace.getBusinessSystemName(), namespace);
								newBusinessSystemNsMap.put(getUnitBusinessSystem(
										grayGdu.getId(), namespace.getBusinessSystemName()), namespace);
							}
						}
						newUnitCloudMap.put(grayGdu.getId(), cloudSpace.getCloudId());
						newGrayGduNsMap.put(grayGdu.getId(), tmpSystemNsMap);
					}
				}

				if (cloudSpace.getGraySdus() != null) {
					for (Sdu graySdu : cloudSpace.getGraySdus()) {
						newGrayUnitDuMap.put(graySdu.getId(), graySdu);
						if (StringUtils.equals(localCloudSpaceId, cloudSpace.getCloudId())) {
							for (UnitNamespace namespace : graySdu.getNamespaces()) {
								newNsUnitMap.put(getCloudSpaceNamespaceId(cloudSpace.getCloudId(),
										namespace.getId()), graySdu.getId());
								// 如果 cloud space 下没有 gdu，对应 gduUnitId 为默认值空字符串
								newBusinessSystemGduMap.put(getUnitBusinessSystem(
										graySdu.getId(), namespace.getBusinessSystemName()), grayGduUnitId);
								newBusinessSystemNsMap.put(getUnitBusinessSystem(
										graySdu.getId(), namespace.getBusinessSystemName()), namespace);
							}
						}
						newUnitCloudMap.put(graySdu.getId(), cloudSpace.getCloudId());
					}
				}

				// 优先读新配置
				if (cloudSpace.getScopeGateways() != null) {
					for (Gateway gateway : cloudSpace.getScopeGateways()) {
						UnitArch.RouteScope routeScope = Optional.ofNullable(gateway.getRouteScope()).orElse(UnitArch.RouteScope.LOCAL_REMOTE);
						switch (routeScope) {
						case LOCAL:
							newLocalOnlyGwMap.putIfAbsent(cloudSpace.getCloudId(), new ConcurrentHashMap<>());
							newLocalOnlyGwMap.get(cloudSpace.getCloudId()).put(gateway.getId(), gateway);
							break;
						default:
							newBusinessSystemGwMap.putIfAbsent(cloudSpace.getCloudId(), new ConcurrentHashMap<>());
							newBusinessSystemGwMap.get(cloudSpace.getCloudId()).put(gateway.getBusinessSystemName(), gateway);
							break;
						}

					}
				}
				else if (cloudSpace.getGateways() != null) {
					for (Gateway gateway : cloudSpace.getGateways()) {
						newBusinessSystemGwMap.putIfAbsent(cloudSpace.getCloudId(), new ConcurrentHashMap<>());
						newBusinessSystemGwMap.get(cloudSpace.getCloudId())
								.put(gateway.getBusinessSystemName(), gateway);
					}
				}

				if (StringUtils.equals(localCloudSpaceId, cloudSpace.getCloudId())) {
					localCloudSpace = cloudSpace;
				}

			}
			allCloudSpaces = cloudSpaces;
			nsUnitMap = newNsUnitMap;
			unitCloudMap = newUnitCloudMap;
			businessSystemGduMap = newBusinessSystemGduMap;
			businessSystemNsMap = newBusinessSystemNsMap;
			businessSystemGwMap = newBusinessSystemGwMap;
			localOnlyGwMap = newLocalOnlyGwMap;
			localUnitId = nsUnitMap.get(getCloudSpaceNamespaceId(localCloudSpaceId, Env.getNamespaceId()));
			grayGduNsMap = newGrayGduNsMap;
			gduNsMap = newGduNsMap;
			grayUnitDuMap = newGrayUnitDuMap;
			allGduMap = newAllGduMap;
		});

		Optional.ofNullable(unitArch).
				map(UnitArch::getTencent).map(TencentUnitArch::getUnitCloudArchitecture).
				map(UnitCloudArch::getBusinessSystems).ifPresent(businessSystemList -> {
					Set<String> newBusinessNameSet = new HashSet<>();
					for (UnitArch.BusinessSystem businessSystem : businessSystemList) {
						newBusinessNameSet.add(businessSystem.getName());
					}
					businessSystemSet = newBusinessNameSet;
				});

		loadArchCallback();

	}

	private static void loadArchCallback() {
		archCallbackRLock.lock();
		for (IUnitChangeCallback callback : archCallbacks) {
			try {
				callback.callback();
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[setUnitArch] arch callback:{}, error msg:{}",
							callback.getName(), e.getMessage(), e);
				}
				else {
					LOGGER.warn("[setUnitArch] arch callback:{}, error msg:{}",
							callback.getName(), e.getMessage());
				}
			}
		}
		archCallbackRLock.unlock();

	}

	public static UnitRouteInfo getUnitRouteRule() {
		return unitRouteInfo;
	}

	public static void setUnitRouteRule(UnitRouteInfo unitRouteInfo) {
		if (!checkUnitRouteRule(unitRouteInfo)) {
			throw new TencentUnitException(ErrorCode.LOAD_ERROR, "parse unit route exception");
		}
		TencentUnitManager.unitRouteInfo = unitRouteInfo;

		Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent).
				map(TencentUnitRouteRule::getUnitRouteRule).
				map(UnitRouteRule::getPassingThroughEnabled).ifPresent(throughEnabled -> {
					contextPassingThroughEnabled = throughEnabled;
				});

		// tagTransforms 目前最多只有一个，list.get(0) 取第一个
		Optional<TransformAction> optionalTransformAction = Optional.ofNullable(unitRouteInfo).
				map(UnitRouteInfo::getTencent).map(TencentUnitRouteRule::getUnitRouteRule).
				map(UnitRouteRule::getTagTransforms).filter(list -> list.size() > 0).
				map(list -> list.get(0)).map(TagTransform::getTransform);

		optionalTransformAction.ifPresent(transformAction -> {
			if (transformAction.getAlgorithm() != null) {
				TransformAlgorithmEnum algorithmEnum =
						TransformAlgorithmEnum.getTransformAlgorith(transformAction.getAlgorithm().getName());
				IUnitTransformAlgorithm unitTransformAlgorithm = null;
				switch (algorithmEnum) {
				case HASHCODE:
					unitTransformAlgorithm = new HashCodeAlgorithm(transformAction.getAlgorithm()
							.getOptions());
					break;
				case MOD:
					unitTransformAlgorithm = new ModAlgorithm(transformAction.getAlgorithm()
							.getOptions());
					break;
				case SUBSTR:
					unitTransformAlgorithm = new SubstrAlgorithm(transformAction.getAlgorithm()
							.getOptions());
					break;
				}
				// TODO: 2023/11/3 支持自定义算法
				transformAction.setUnitTransformAlgorithm(unitTransformAlgorithm);

				tagTransform = unitRouteInfo.getTencent().getUnitRouteRule().getTagTransforms().get(0);
				unitRoutes = unitRouteInfo.getTencent().getUnitRouteRule().getUnitRoutes();
			}
		});

		Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent)
				.map(TencentUnitRouteRule::getGrayUnitRouteRule).map(
						GrayUnitRouteRule::getUnitRoutes).ifPresent(grayUnitRouteList -> {
							grayUnitRoutes = grayUnitRouteList;
							Set<String> newGrayUnitHeaderKey = new HashSet<>();
							for (GrayUnitRoute route : grayUnitRoutes) {
								for (UnitTag unitTag : route.getMatch().getConditions()) {
									switch (unitTag.getTagPosition()) {
									case HEADER:
										newGrayUnitHeaderKey.add(unitTag.getTagField());
										break;
									}
								}
							}
							grayUnitHeaderKey = newGrayUnitHeaderKey;
						}
				);

		Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent).
				map(TencentUnitRouteRule::getRouterIdentifier).ifPresent(identifier -> {
					if (StringUtils.isEmpty(identifier.getKeySeparator())) {
						identifier.setKeySeparator(DEFAULT_KEY_SEPARATOR);
					}
					if (StringUtils.isEmpty(identifier.getKeyValueSeparator())) {
						identifier.setKeyValueSeparator(DEFAULT_KEY_VALUE_SEPARATOR);
					}
					if (StringUtils.isEmpty(identifier.getShardingIdentifierKey())) {
						identifier.setShardingIdentifierKey(DEFAULT_SHARDING_IDENTIFIER_KEY);
					}

					if (StringUtils.isEmpty(identifier.getRouterIdentifierHeader())) {
						identifier.setRouterIdentifierHeader(DEFAULT_ROUTER_IDENTIFIER_HEADER);
					}

					Map<String, MappingService> mappingServiceMap = new HashMap<>();
					if (identifier.getShardingIdentifierMappingServices() != null) {
						for (MappingService mappingService : identifier.getShardingIdentifierMappingServices()) {
							mappingServiceMap.put(mappingService.getCloudId(), mappingService);
						}
					}
					identifier.setMappingServiceMap(mappingServiceMap);

					routerIdentifier = identifier;
				});

		Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent).ifPresent(tencentUnitRouteRule -> {
			Map<String, String> newFailoverUnitIdMap = new ConcurrentHashMap<>();
			Map<String, Boolean> newActiveSduIdMap = new ConcurrentHashMap<>();
			Set<String> newDisableZoneSet = new HashSet<>();
			String newLocalFailoverGduUnitId = null;
			if (tencentUnitRouteRule.getGduRouteRule() != null) {
				for (UnitRoute gduRouteRule : tencentUnitRouteRule.getGduRouteRule()) {
					// 暂时都是只有一个 CloudId, operator 只有 and
					Optional<UnitTag> matchTag = gduRouteRule.getMatch().getConditions().stream().
							filter(cond -> UnitTagEngine.matchTag(cond, localCloudSpaceId)).findFirst();
					if (matchTag.isPresent()) {
						String scope = Optional.ofNullable(gduRouteRule).map(UnitRoute::getRoute)
								.map(MatchRoute::getFailover).map(MatchRouteFailover::getScope).orElse("");
						switch (scope) {
						case "region":
							if (Boolean.TRUE.equals(gduRouteRule.getRoute().getFailover().getEnabled())) {
								LOGGER.info("use gdu failover mode, match name:{}, origin gdu route unit:{}, failover unit:{}",
										gduRouteRule.getMatch().getName(), gduRouteRule.getRoute()
												.getUnitId(), gduRouteRule.getRoute().getFailover().getRoute()
												.getUnitId());
								newLocalFailoverGduUnitId = gduRouteRule.getRoute().getFailover().getRoute()
										.getUnitId();
								newFailoverUnitIdMap.put(gduRouteRule.getRoute()
										.getUnitId(), newLocalFailoverGduUnitId);
							}
							break;
						case "zone":
							for (UnitTag unitTag : gduRouteRule.getRoute().getFailover().getRoute().getLocationFilter()
									.getZoneFilter().getConditions()) {
								newDisableZoneSet.add(unitTag.getTagValue());
							}
							LOGGER.info("use zone failover mode, match name:{}, newDisableZoneSet:{}", gduRouteRule.getMatch()
									.getName(), newDisableZoneSet);
							break;
						}
						break;
					}
				}
			}
			if (tencentUnitRouteRule.getUnitRouteRule() != null) {
				for (UnitRoute unitRoute : tencentUnitRouteRule.getUnitRouteRule().getUnitRoutes()) {
					MatchRouteFailover failover = unitRoute.getRoute().getFailover();
					if (failover != null && Boolean.TRUE.equals(failover.getEnabled())) {
						newFailoverUnitIdMap.put(unitRoute.getRoute().getUnitId(), failover.getRoute().getUnitId());
						newActiveSduIdMap.put(failover.getRoute().getUnitId(), true);
					}
					else {
						newActiveSduIdMap.put(unitRoute.getRoute().getUnitId(), true);
					}
				}
			}
			failoverUnitIdMap = newFailoverUnitIdMap;
			disableZoneSet = newDisableZoneSet;
			localFailoverGduUnitId = newLocalFailoverGduUnitId;
			activeSduIdMap = newActiveSduIdMap;
		});

		loadRuleCallback();

	}

	private static void loadRuleCallback() {
		ruleCallbackRLock.lock();
		try {
			for (IUnitChangeCallback callback : ruleCallbacks) {
				callback.callback();
			}
		}
		finally {
			ruleCallbackRLock.unlock();
		}
	}

	public static Map<String, String> getFailoverUnitIdMap() {
		return failoverUnitIdMap;
	}

	public static void setUnitGray(UnitGray unitGray) {
		Optional.ofNullable(unitGray).map(UnitGray::getTencent).map(TencentUnitGray::getUnitGrayList)
				.map(UnitGrayList::getIds).ifPresent(ids -> {
					unitGrayIds = new HashSet<>(ids);
				});
	}

	public static Set<String> getUnitGrayIds() {
		return unitGrayIds;
	}

	public static IUnitTransformAlgorithm getCustomAlgorithm() {
		return customAlgorithm;
	}

	public static void setCustomAlgorithm(IUnitTransformAlgorithm customAlgorithm) {
		TencentUnitManager.customAlgorithm = customAlgorithm;
	}

	public static String getCloudSpaceNamespaceId(String cloudSpaceId, String namespaceId) {
		return String.format("cn:%s@%s", cloudSpaceId, namespaceId);
	}

	public static String getUnitBusinessSystem(String unitId, String businessSystemName) {
		return String.format("ub:%s@%s", unitId, businessSystemName);
	}

	public static String getCloudBusinessSystem(String cloudSpaceId, String businessSystemName) {
		return String.format("cb:%s@%s", cloudSpaceId, businessSystemName);
	}

	public static String getLocalCloudSpaceId() {
		return localCloudSpaceId;
	}

	public static boolean isLocalCloudSpace(String targetCloudId) {
		return StringUtils.equals(targetCloudId, getLocalCloudSpaceId());
	}

	public static String getLocalUnitId() {
		return localUnitId;
	}

	public static String getGduUnitId() {
		if (StringUtils.isNotEmpty(localFailoverGduUnitId)) {
			return localFailoverGduUnitId;
		}
		else {
			return localGduUnitId;
		}
	}

	public static TagTransform getTagTransform() {
		return tagTransform;
	}

	public static Map<String, String> getNsUnitMap() {
		return nsUnitMap;
	}

	public static Map<String, String> getUnitCloudMap() {
		return unitCloudMap;
	}

	public static Map<String, String> getBusinessSystemGduMap() {
		return businessSystemGduMap;
	}

	public static List<UnitRoute> getUnitRoutes() {
		return unitRoutes;
	}

	public static CloudSpace getLocalCloudSpace() {
		return localCloudSpace;
	}

	/**
	 * 判断 unit id 是否存在，可查询其他 cloud 的.
	 */
	public static boolean checkUnitId(String unitId) {
		return getUnitCloudMap().containsKey(unitId);
	}

	/**
	 * 通过 customerIdentifier（客户要素）获取匹配的单元化路由(从 unitRoutes 中匹配).
	 * 若获取不到RoutingUnit，返回null
	 */
	public static RoutingUnit getRoutingUnit(String customerIdentifier) {
		// 根据客户要素（routerIdentifier）获取客户号
		String customerNumber = TencentUnitManager.getCustomerNumber(customerIdentifier);
		// 根据客户号，进行hash等计算，得到转换值（分片号）
		String targetTagValue = getTransformTargetTagValue(customerNumber);
		if (targetTagValue == null) {
			LOGGER.warn("[unit] customerIdentifier:{}, customerNumber: {}, transfer to target tag is null",
					customerIdentifier, customerNumber);
			return null;
		}

		// 根据分片信息计算出匹配的单元信息
		MatchRoute matchRoute = getMatchRoute(customerIdentifier, customerNumber, targetTagValue);
		if (matchRoute != null) {
			// 返回路由单元信息，包含了中间计算的数据，便于上层调用方回溯和使用（RoutingUnitContext会使用）
			return new RoutingUnit(customerNumber, targetTagValue, matchRoute);
		}

		LOGGER.warn("[unit] customerIdentifier: {}, customerNumber: {}, not match any rule",
				customerIdentifier, customerNumber);
		return null;
	}

	/**
	 * 根据taValue（sharingKey）计算出对应的匹配的单元信息（MatchRoute）.
	 *
	 * @param customerIdentifier 客户要素
	 * @param customerNumber     客户号
	 * @param tagValue           分片号
	 * @return 出现匹配不上，返回null
	 */
	public static MatchRoute getMatchRoute(String customerIdentifier, String customerNumber, String tagValue) {
		// 从路由规则里获取tagName
		String tagName = Optional.ofNullable(getTagTransform()).
				map(tagTransform -> tagTransform.getDestination()).
				map(destination -> destination.getTagName()).orElse(null);

		// 路由匹配
		Map<String, String> targetTagMap = new HashMap<>();
		targetTagMap.put(tagName, tagValue);
		for (UnitRoute route : getUnitRoutes()) {
			if (UnitTagEngine.checkUnitRouteHit(route, targetTagMap)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[checkUnitRouteListHit] customer({}, {}) tag({}) match route unit id:{}",
							customerIdentifier, customerNumber, targetTagMap, route.getRoute().getActualUnitId());
				}
				return route.getRoute();
			}
		}

		// 没有匹配到规则的 tag，返回null，由上层决定是否抛出异常
		return null;
	}

	public static String getTransformTargetTagValue(String cid) {
		return Optional.ofNullable(getTagTransform()).
				map(tagTransform -> tagTransform.getTransform()).
				map(transform -> transform.getUnitTransformAlgorithm()).
				map(algorithm -> algorithm.transform(cid)).orElse(null);
	}

	/**
	 * 根据客户要素，获取客户号.
	 * 客户要素格式：k1=v1,k2=v2,k3=v3，客户号的k=CustomerNumber
	 *
	 * @param cid 客户要素
	 * @return String 客户号
	 */
	public static String getCustomerNumber(String cid) {
		// 解释k1=v1,k2=v2...
		String[] kvList = StringUtils.split(cid, routerIdentifier.getKeySeparator());

		// 找一下是否存在CustomerNumber
		Map<String, String> kvMap = new HashMap<>(kvList.length);
		for (String value : kvList) {

			String[] kv = StringUtils.split(value, routerIdentifier.getKeyValueSeparator(), 2);
			if (kv.length != 2) {
				String msg = String.format("customerIdentifier(%s) format error, key separator:(%s), key value separator:(%s)",
						cid, routerIdentifier.getKeySeparator(), routerIdentifier.getKeyValueSeparator());
				throw new TencentUnitException(ErrorCode.CUSTOMER_IDENTIFIER_TRANSFORM_ERROR, msg);
			}

			kvMap.put(kv[0], kv[1]);
		}
		if (kvMap.containsKey(routerIdentifier.getShardingIdentifierKey())) {
			return kvMap.get(routerIdentifier.getShardingIdentifierKey());
		}

		if (MappingServiceLoader.getService() == null) {
			String msg = String.format("[unit] cid:%s, not found key(%s) and not found mapping service", cid, routerIdentifier.getShardingIdentifierKey());
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.MAPPING_SERVICE_EMPTY_ERROR, msg);
		}

		MappingEntity message = MappingServiceLoader.getService().processMapping(cid);
		if (message == null || StringUtils.isEmpty(message.getCustomerNumber())) {
			// 如果使用的是 CustomerMappingService，这里只会是 empty customer number
			String msg = String.format("invoke mapping service get empty customer, cid:%s", cid);
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.MAPPING_RESPONSE_CUSTOMER_NUMBER_EMPTY_ERROR, msg);
		}
		return message.getCustomerNumber();
	}

	/**
	 * 只能匹配当前 cloud 的.
	 */
	public static UnitNamespace getUnitNamespaceId(String unitId, String systemName) {
		// businessSystemNsMap 包含 gdu 和 sdu 的
		return businessSystemNsMap.get(getUnitBusinessSystem(unitId, systemName));
	}

	public static boolean isEnable() {
		return enable;
	}

	public static void setEnable(boolean enable) {
		TencentUnitManager.enable = enable;
	}

	public static boolean checkLocalNamespace(String nsId) {
		return StringUtils.equals(Env.getNamespaceId(), nsId);
	}

	public static Gateway getBusinessSystemGateway(String cloudId, String system) {
		return businessSystemGwMap.getOrDefault(cloudId, Collections.emptyMap()).get(system);
	}

	public static Gateway getLocalBusinessSystemGateway(String system) {
		return businessSystemGwMap.getOrDefault(getLocalCloudSpaceId(), Collections.emptyMap()).get(system);
	}

	public static Map<String, Gateway> getLocalBusinessSystemGwMap() {
		return businessSystemGwMap.getOrDefault(getLocalCloudSpaceId(), Collections.emptyMap());
	}

	public static Map<String, Gateway> getLocalOnlyGwMap() {
		return localOnlyGwMap.getOrDefault(getLocalCloudSpaceId(), Collections.emptyMap());
	}

	public static void addArchCallback(IUnitChangeCallback callback) {
		archCallbackWLock.lock();
		try {
			if (!archCallbacks.contains(callback)) {
				archCallbacks.add(callback);
			}
		}
		finally {
			archCallbackWLock.unlock();
		}

		loadArchCallback();
		loadRuleCallback();
	}

	public static void addRuleCallback(IUnitChangeCallback callback) {
		ruleCallbackWLock.lock();
		try {
			if (!ruleCallbacks.contains(callback)) {
				ruleCallbacks.add(callback);
			}
		}
		finally {
			ruleCallbackWLock.unlock();
		}

		loadArchCallback();
		loadRuleCallback();
	}

	public static RouterIdentifier getRouterIdentifier() {
		return routerIdentifier;
	}

	public static Map<String, Map<String, UnitNamespace>> getGrayGduNsMap() {
		return grayGduNsMap;
	}

	public static Map<String, Map<String, UnitNamespace>> getGduNsMap() {
		return gduNsMap;
	}

	public static Set<String> getBusinessSystemSet() {
		return businessSystemSet;
	}

	public static String getRouterIdentifierHeader() {
		if (routerIdentifier != null) {
			return routerIdentifier.getRouterIdentifierHeader();
		}
		else {
			return DEFAULT_ROUTER_IDENTIFIER_HEADER;
		}
	}

	public static String getLocalCloudMappingApi() {
		String result = Optional.ofNullable(routerIdentifier).map(RouterIdentifier::getMappingServiceMap)
				.map(m -> m.get(localCloudSpaceId)).map(MappingService::getApiPath).orElse(null);
		// sdk 兼容 mapping url 从 arch 获取
		if (StringUtils.isEmpty(result)) {
			result = Optional.ofNullable(TencentUnitManager.getLocalCloudSpace()).
					map(CloudSpace::getMappingServiceUrl).orElse(null);
		}
		return result;
	}

	public static List<GrayUnitRoute> getGrayUnitRoutes() {
		return grayUnitRoutes;
	}

	public static boolean inGrayUnit() {
		return grayUnitDuMap.containsKey(getLocalUnitId());
	}

	public static String getGraySduUnitId() {
		List<Sdu> graySdus = Optional.ofNullable(localCloudSpace)
				.map(CloudSpace::getGraySdus).orElse(Collections.emptyList());
		for (Sdu graySdu : graySdus) {
			// 不为空时返回第一个
			return graySdu.getId();
		}
		return null;
	}


	public static Set<String> getGrayUnitHeaderKey() {
		return grayUnitHeaderKey;
	}

	public static boolean isContextPassingThroughEnabled() {
		return contextPassingThroughEnabled;
	}

	private static boolean checkUnitArch(UnitArch unitArch) {
		try {
			String localCloudId = unitArch.getTencent().getUnitCloudArchitecture().getLocalCloudId();
			if (StringUtils.isEmpty(localCloudId)) {
				LOGGER.warn("parse unit arch error, empty localCloudId");
				return false;
			}
			Set<String> cloudIds = new HashSet<>();
			for (CloudSpace cloudSpace : unitArch.getTencent().getUnitCloudArchitecture().getCloudSpaces()) {
				if (StringUtils.isEmpty(cloudSpace.getCloudId())) {
					LOGGER.warn("parse unit arch error, empty cloudId");
					return false;
				}
				cloudIds.add(cloudSpace.getCloudId());
			}
			if (!cloudIds.contains(localCloudId)) {
				LOGGER.warn("parse unit arch error, localCloudId:{} not in cloudIds:{}", localCloudId, cloudIds);
				return false;
			}

			return true;
		}
		catch (Exception e) {
			LOGGER.warn("parse unit arch exception:{}", e.getMessage());
			return false;
		}
	}

	private static boolean checkUnitRouteRule(UnitRouteInfo unitRouteInfo) {
		try {
			AtomicBoolean pass = new AtomicBoolean(true);
			Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent)
					.map(TencentUnitRouteRule::getGrayUnitRouteRule).map(
							GrayUnitRouteRule::getUnitRoutes).ifPresent(grayUnitRouteList -> {

								for (GrayUnitRoute route : grayUnitRouteList) {
									for (UnitTag unitTag : route.getMatch().getConditions()) {
										switch (unitTag.getTagPosition()) {
										// 暂时只支持 header
										case HEADER:
											break;
										default:
											LOGGER.warn("parse unit tag position error, not support:{} ", unitTag.getTagPosition());
											pass.set(false);
										}
									}

									if (route.getRoute().getUnits().size() != 2) {
										LOGGER.warn("gray match route units count error");
										pass.set(false);
									}
								}
							}
					);

			// 开启 failover，打印日志
			Optional.ofNullable(unitRouteInfo).map(UnitRouteInfo::getTencent)
					.map(TencentUnitRouteRule::getUnitRouteRule)
					.map(UnitRouteRule::getUnitRoutes).ifPresent(unitRoutesList -> {
						for (UnitRoute unitRoute : unitRoutesList) {
							MatchRouteFailover failover = unitRoute.getRoute().getFailover();
							if (failover != null && Boolean.TRUE.equals(failover.getEnabled())) {
								LOGGER.info("use failover mode, match name:{}, origin route unit:{}, failover unit:{}",
										unitRoute.getMatch().getName(), unitRoute.getRoute().getUnitId(), failover.getRoute()
												.getUnitId());
							}
						}
					});

			return pass.get();
		}
		catch (Exception e) {
			LOGGER.warn("parse unit arch exception:{}", e.getMessage());
			return false;
		}
	}

	/**
	 * 返回本 cloud space 里业务系统所在的全局单元（GDU）的单元号.
	 */
	public static String getGlobalUnitId(String systemName) {
		return TencentUnitManager.getBusinessSystemGduMap().get(
				TencentUnitManager.getUnitBusinessSystem(
						TencentUnitManager.getLocalUnitId(), systemName));
	}

	public static Set<String> getDisableZoneSet() {
		return disableZoneSet;
	}

	public static Map<String, Gdu> getAllGduMap() {
		return allGduMap;
	}

	public static List<CloudSpace> getAllCloudSpaces() {
		return allCloudSpaces;
	}

	public static Map<String, Boolean> getActiveSduIdMap() {
		return activeSduIdMap;
	}
}
