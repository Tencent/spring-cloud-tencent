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

package com.tencent.cloud.plugin.unit.utils;

import java.util.List;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.EnvironmentUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.UnitTagEngine;
import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;
import com.tencent.tsf.unit.core.model.RoutingUnit;
import com.tencent.tsf.unit.core.model.UnitArch.Gateway;
import com.tencent.tsf.unit.core.model.UnitNamespace;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayMatchRoute;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayMatchRouteUnit;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayUnitRoute;
import com.tencent.tsf.unit.core.utils.TencentUnitUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.ServiceInstance;

public final class SpringCloudUnitUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudUnitUtils.class);

	private static PolarisDiscoveryClient discoveryClient;

	private SpringCloudUnitUtils() {
	}

	/**
	 * 单元化才会进入这里, feign/rest template 请求前的预处理, 判断是否需要计算客户号, 是否转发网关, 判断结果记录在 TencentUnitContext.
	 */
	public static void preRequestRecordUnitContext(String serviceName) {
		if (discoveryClient == null) {
			discoveryClient = ApplicationContextAwareUtils.getBean(PolarisDiscoveryClient.class);
		}

		TencentUnitContext.putSystemTagsFromUser();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[preRequestRecordUnitContext] service name:{}, unit context:{}",
					serviceName, TencentUnitContext.getOriginCompositeContextMap());
		}
		String system = TencentUnitContext.getSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM);
		// 无论 gdu 还是 sdu, 都需要 system 信息，如果没有 system（返回 false），则直接返回
		if (!checkSystem(system, serviceName)) {
			return;
		}

		// 先进行灰度判断
		RoutingUnit routingUnit = preRequestRecordGrayUnitContext();
		String customerIdentifier = TencentUnitContext.getSystemTag(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER);

		// 如果 dest unit id 存在，则直接设置 unit context 并返回
		if (preRequestRecordIfDestUnitExist(system, customerIdentifier, serviceName)) {
			return;
		}

		// 发生 gdu 容灾切换，gdu 不在同一个 cloud 的，直接设置 unit context 并返回
		if (preRequestRecordIfGduNotInLocalCloud(serviceName)) {
			return;
		}

		boolean toSdu = true;
		String gduUnitId = getGduUnitId();

		UnitNamespace gduNs = TencentUnitManager.getUnitNamespaceId(gduUnitId, system);
		boolean gduNsNotExist = (gduNs == null || StringUtils.isEmpty(gduNs.getId()));

		// preRequestRecordIfDestUnitExist 可能需要 gdu 的信息,所以需要这里进行解析 gdu 和设置
		if (gduNsNotExist) {
			if (StringUtils.isEmpty(customerIdentifier)) {
				String msg = String.format("[preRequestRecordUnitContext] gdu system not exist:%s, and customerIdentifier is empty, serviceName:%s", system, serviceName);
				LOGGER.error(msg);
				throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
			}
			// 没有 gdu 时，肯定是 sdu forward
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_SDU_FORWARD_ONLY, Boolean.TRUE.toString());
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_FORWARD_ONLY, Boolean.FALSE.toString());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordUnitContext] gduNsNotExist. only forward to sdu, system:{}, service name:{}, unit context:{}",
						system, serviceName, TencentUnitContext.getOriginCompositeContextMap());
			}
		}
		else {
			// 有 gdu 对应 ns
			// 没有客户要素，限定只转发到 gdu
			if (StringUtils.isEmpty(customerIdentifier)) {
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_FORWARD_ONLY, Boolean.TRUE.toString());
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_SDU_FORWARD_ONLY, Boolean.FALSE.toString());
				// 前面校验了，这里直接 set context
				setUnitContext(gduUnitId, gduNs, serviceName);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[preRequestRecordUnitContext] cid or business system is empty. only forward to gdu, service name:{}, unit context:{}",
							serviceName, TencentUnitContext.getOriginCompositeContextMap());
				}
				return;
			}

			String gduServiceId = getGduServiceId(serviceName, gduNs.getId());
			List<ServiceInstance> gduServices = discoveryClient.getInstances(gduServiceId);
			// gdu 有实例，直接转入 gdu，不需要计算客户号
			if (CollectionUtils.isNotEmpty(gduServices)) {
				toSdu = false;
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_INSTANCE_EXIST, Boolean.TRUE.toString());
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_FORWARD_ONLY, Boolean.TRUE.toString());
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_SDU_FORWARD_ONLY, Boolean.FALSE.toString());
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SERVICE, serviceName);
				setUnitContext(gduUnitId, gduNs, serviceName);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[preRequestRecordUnitContext] gdu first and exist instance. forward to gdu, service name:{}, unit context:{}",
							serviceName, TencentUnitContext.getOriginCompositeContextMap());
				}
			}
		}

		if (toSdu) {
			String graySduUnitId = TencentUnitContext.getGraySystemTag(TencentUnitContext.GRAY_CLOUD_SPACE_SDU_UNIT_ID);
			if (StringUtils.isNotEmpty(graySduUnitId)) {
				// gray sdu, 直接获取 unit id, 不需要解析客户要素
				checkUnitIdAndSetContext(graySduUnitId, system, serviceName);
			}
			else {
				// 普通 sdu。灰度时可能计算过客户号，这里就不重复计算
				if (routingUnit == null) {
					routingUnit = TencentUnitManager.getRoutingUnit(customerIdentifier);
				}
				if (checkUnitIdAndSetContext(routingUnit, system, customerIdentifier, serviceName)) {
					// 增加上下文，记录目标单元号和目标的shardingKey，传递到下游
					TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_CUSTOMER_NUMBER, routingUnit.getCustomerNumber());
					TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SHARDING_KEY, routingUnit.getShardingKey());
				}
				else {
					// sdu 没有对应 ns，前面 gdu 没有对应实例，预期后面会提示没有实例的错误
					TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_FORWARD_ONLY, Boolean.TRUE.toString());
				}
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordUnitContext] do calculate customer num. service name:{}, unit context:{}",
						serviceName, TencentUnitContext.getOriginCompositeContextMap());
			}
		}
	}

	public static boolean checkSystem(String system, String serviceName) {
		// 有业务系统时，才需要判断
		if (StringUtils.isEmpty(system)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordUnitContext] system is empty, serviceName:{}", serviceName);
			}
			return false;
		}

		if (!TencentUnitManager.getBusinessSystemSet().contains(system)) {
			String msg = String.format("[preRequestRecordUnitContext] system is empty or not exist:%s, serviceName:%s", system, serviceName);
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
		}
		return true;
	}

	public static boolean preRequestRecordIfGduNotInLocalCloud(String serviceName) {
		String gduUnitId = getGduUnitId();
		boolean gduInLocalCloud = TencentUnitUtils.checkLocalCloudSpace(gduUnitId);

		if (gduInLocalCloud) {
			return false;
		}

		setCrossGduContext(gduUnitId, serviceName);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[preRequestRecordUnitContext] gdu in other cloud, gdu unit id:{}, service name:{}, unit context:{}",
					gduUnitId, serviceName, TencentUnitContext.getOriginCompositeContextMap());
		}
		return true;
	}

	public static boolean preRequestRecordIfDestUnitExist(String system, String customerIdentifier, String serviceName) {
		String destUnitId = TencentUnitContext.getSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID);
		if (StringUtils.isNotEmpty(destUnitId)) {
			String failoverUnitId = TencentUnitManager.getFailoverUnitIdMap().get(destUnitId);
			if (StringUtils.isNotEmpty(failoverUnitId)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[preRequestRecordUnitContext] use failover unit id:{}, origin unit id:{}, service name:{}",
							failoverUnitId, destUnitId, serviceName);
				}
				TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID, failoverUnitId);
				destUnitId = failoverUnitId;
			}

			if (checkUnitIdAndSetContext(destUnitId, system, serviceName)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("[preRequestRecordUnitContext] unit id exist:{}. service name:{}, unit context:{}",
							destUnitId, serviceName, TencentUnitContext.getOriginCompositeContextMap());
				}
				return true;
			}
			else {
				String msg = String.format("[preRequestRecordUnitContext] destUnitId ns not exist:%s, customerIdentifier:%s,"
						+ " system:%s, serviceName:%s", destUnitId, customerIdentifier, system, serviceName);
				LOGGER.error(msg);
				throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
			}
		}
		return false;
	}

	/**
	 * 灰度 unit context 预处理.
	 * @return 如果需要计算客户号，则返回 RoutingUnit，避免重复计算
	 */
	public static RoutingUnit preRequestRecordGrayUnitContext() {
		String sourceGrayUnitInfo = TencentUnitContext.getSourceTag(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO);

		if (StringUtils.isEmpty(sourceGrayUnitInfo) && CollectionUtils.isEmpty(TencentUnitManager.getGrayUnitRoutes())) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordGrayUnitContext] empty header gray info and empty gray unit routes,"
						+ "unitContextMap:{}", TencentUnitContext.getOriginCompositeContextMap());
			}
			return null;
		}

		if (StringUtils.isNotEmpty(sourceGrayUnitInfo)) {
			// 已经是灰度单元的，直接转
			List<GrayMatchRouteUnit> grayMatchRouteUnitList = TencentUnitContext.parseGrayMatchRouteUnitList();
			TencentUnitContext.setGrayUnitContext(grayMatchRouteUnitList);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordGrayUnitContext] using header gray, unitContextMap:{}",
						TencentUnitContext.getOriginCompositeContextMap());
			}
			return null;
		}

		// 判断是否存在潜在的 gray context，如果不存在则一定不是灰度
		if (TencentUnitContext.grayContextIsEmpty()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[preRequestRecordGrayUnitContext] empty gray header, unitContextMap:{}",
						TencentUnitContext.getOriginCompositeContextMap());
			}
			return null;
		}
		// 存在灰度规则的相关 header，判断是否匹配
		GrayMatchRoute grayMatchRoute = null;
		RoutingUnit routingUnit = null;
		String customerIdentifier = TencentUnitContext.getSystemTag(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER);

		for (GrayUnitRoute grayUnitRoute : TencentUnitManager.getGrayUnitRoutes()) {
			if (UnitTagEngine.checkGrayUnitRouteHit(grayUnitRoute)) {
				boolean match = false;
				if (Boolean.TRUE.equals(grayUnitRoute.getMatch().getGrayListEnabled())) {
					// 还需要命中需要灰度名单
					if (StringUtils.isNotEmpty(customerIdentifier)) {
						if (routingUnit == null) {
							// 客户号只获取一次
							routingUnit = TencentUnitManager.getRoutingUnit(customerIdentifier);
						}
						if (routingUnit != null && TencentUnitManager.getUnitGrayIds()
								.contains(routingUnit.getCustomerNumber())) {
							match = true;
						}
						else {
							LOGGER.debug("[preRequestRecordGrayUnitContext] match routes, not in gray list");
						}
					}
				}
				else {
					match = true;
				}
				if (match) {
					grayMatchRoute = grayUnitRoute.getRoute();
					break;
				}
			}
		}
		// 匹配
		if (grayMatchRoute != null) {
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO,
					JacksonUtils.serialize2Json(grayMatchRoute.getUnits()));
			TencentUnitContext.setGrayUnitContext(grayMatchRoute.getUnits());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[preRequestRecordGrayUnitContext] grayMatchRoute:{}, unitContextMap:{}",
					grayMatchRoute, TencentUnitContext.getOriginCompositeContextMap());
		}
		return routingUnit;
	}

	/**
	 * 如果没有 gdu 对应 ns，返回 null；SCT 统一返回 nsId/serviceName.
	 */
	public static String getGduServiceId(String serviceName, String gduNsId) {
		if (StringUtils.isEmpty(gduNsId)) {
			return null;
		}
		return gduNsId + "/" + serviceName;
	}

	public static String getGduUnitId() {
		String gduUnitId;
		if (StringUtils.isNotEmpty(TencentUnitContext.getGraySystemTag(TencentUnitContext.GRAY_CLOUD_SPACE_GDU_UNIT_ID))) {
			gduUnitId = TencentUnitContext.getGraySystemTag(TencentUnitContext.GRAY_CLOUD_SPACE_GDU_UNIT_ID);
		}
		else {
			gduUnitId = TencentUnitManager.getGduUnitId();
		}
		return gduUnitId;
	}

	/**
	 * 【网关使用该方法】从consumer过来的feign或restTemplate调用/或其他 cloud gw 转过来的，从header中取已经计算好的ns以及token做校验.
	 */
	public static void processFromUnitHeader(Map<String, String> unitMap, String path) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[getProxyInfoFromUnitHeader] parse unit from header. path:{}, origin unit map:{}", path, unitMap);
		}

		String targetCloudId = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_CLOUD);
		if (TencentUnitManager.isLocalCloudSpace(targetCloudId)) {
			updateLocalCloudUnitContext(unitMap);
		}
		else {
			updateCrossCloudUnitContext(unitMap);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[getProxyInfoFromUnitHeader] parse unit from header. path:{}, unit context:{}",
					path, TencentUnitContext.getOriginCompositeContextMap());
		}
	}

	/**
	 * 下一跳是其他 cloud 的 gw.
	 */
	private static void updateCrossCloudUnitContext(Map<String, String> unitMap) {
		String destUnitId = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID);
		String destServiceName = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_SERVICE);
		String destSystem = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM);

		if (!TencentUnitManager.checkUnitId(destUnitId)) {
			// 非当前 cloud, 只需要检查是否存在
			String msg = String.format("[getProxyInfoFromUnitHeader] check unit id error, destUnitId:%s, destServiceName:%s",
					destUnitId, destServiceName);
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
		}

		unitMap.put(TencentUnitContext.CLOUD_SPACE_TARGET_CLOUD, TencentUnitManager.getUnitCloudMap().get(destUnitId));

		Gateway gateway = getGateway(destUnitId, destSystem);
		TencentUnitContext.putRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY, gateway);
		TencentUnitContext.putSystemTags(unitMap);
	}

	/**
	 * 下一跳是本 cloud 的服务.
	 */
	private static void updateLocalCloudUnitContext(Map<String, String> unitMap) {
		String destUnitId = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID);
		String destServiceName = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_SERVICE);

		if (StringUtils.isEmpty(destUnitId)) {
			TencentUnitContext.putSystemTags(unitMap);
			// 是否跨 cloud gdu 优先访问，此时没有目标单元
			String grayUnitInfo = unitMap.get(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO);
			// 设置灰度上游
			if (StringUtils.isNotEmpty(grayUnitInfo)) {
				TencentUnitContext.putSourceTag(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO, grayUnitInfo);
			}
			SpringCloudUnitUtils.preRequestRecordUnitContext(destServiceName);
		}
		else {
			String destSystemName = unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM);

			// 在当前 cloud, check ns
			UnitNamespace namespace = TencentUnitManager.getUnitNamespaceId(destUnitId, destSystemName);
			if (namespace == null) {
				String msg = String.format("[getProxyInfoFromUnitHeader] check ns error, destUnitId:%s, destServiceName:%s",
						destUnitId, destServiceName);
				LOGGER.error(msg);
				throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
			}
			TencentUnitContext.putSystemTags(unitMap);
			TencentUnitContext.putRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_TARGET_NAMESPACE_ID, namespace.getId());
		}
	}

	/**
	 * 【网关使用该方法】通过http直接调用网关，从header获取 cid 并解析.
	 */
	public static void processFromCid(String cid, String path) {

		//url：/groupContext/system/ms/api
		String serviceName;
		String systemName;
		String[] pathSegments = path.split("/");
		if (pathSegments.length >= 5) {
			serviceName = pathSegments[3];
			systemName = pathSegments[2];
			if (StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(systemName)) {
				String msg = String.format("[checkUnitFromHttp] serviceName:%s or systemName:%s is empty", serviceName, systemName);
				throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
			}
		}
		else {
			String msg = "[checkUnitFromHttp] path pattern is wrong, path:" + path;
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
		}
		TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER, cid);
		TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM, systemName);

		SpringCloudUnitUtils.preRequestRecordUnitContext(serviceName);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[getProxyInfoFromCid] parse unit from cid. path:{}, unit context:{}",
					path, TencentUnitContext.getOriginCompositeContextMap());
		}
	}

	private static boolean checkUnitIdAndSetContext(RoutingUnit routingUnit, String system, String customerIdentifier, String serviceName) {
		if (routingUnit == null || StringUtils.isEmpty(routingUnit.getUnitId())) {
			String msg = String.format("[preRequestRecordUnitContext] routingUnit unit id not exist, customerIdentifier:%s, system:%s, serviceName:%s", customerIdentifier, system, serviceName);
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
		}

		return checkUnitIdAndSetContext(routingUnit.getUnitId(), system, serviceName);
	}

	private static boolean checkUnitIdAndSetContext(String unitId, String system, String serviceName) {
		if (TencentUnitUtils.checkLocalCloudSpace(unitId)) {
			// 本地，检查 ns 是否存在
			UnitNamespace namespace = TencentUnitManager.getUnitNamespaceId(unitId, system);
			if (namespace != null) {
				setUnitContext(unitId, namespace, serviceName);
				// 设置治理 ns
				MetadataContext metadataContext = MetadataContextHolder.get();
				metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
						MetadataConstant.POLARIS_TARGET_NAMESPACE, namespace.getId());
				return true;
			}
			else {
				return false;
			}
		}
		else if (TencentUnitManager.checkUnitId(unitId)) {
			// 异地，检查 unitId 是否存在
			setUnitContext(unitId, serviceName);
			return true;
		}
		else {
			return false;
		}
	}

	private static void setUnitContext(String unitId, String serviceName) {
		setUnitContext(unitId, null, serviceName);
	}

	private static void setUnitContext(String unitId, UnitNamespace namespace, String serviceName) {
		TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID, unitId);
		if (namespace != null) {
			TencentUnitContext.putRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_TARGET_NAMESPACE_ID, namespace.getId());
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_NAMESPACE_ID, namespace.getId());
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_NAMESPACE_NAME, namespace.getName());
		}

		// 不同 cloud, 需要转到对应 gateway
		if (!TencentUnitUtils.checkLocalCloudSpace(unitId)) {
			Gateway gateway = getGateway(unitId);

			TencentUnitContext.putRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY, gateway);

			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SERVICE, serviceName);
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_CLOUD, TencentUnitManager.getUnitCloudMap()
					.get(unitId));
		}
	}

	/**
	 * 设置跨 cloud gdu 的上下文, 然后再 gdu first 访问，暂不确定目标单元.
	 */
	private static void setCrossGduContext(String unitId, String serviceName) {
		Gateway gateway = getGateway(unitId);
		TencentUnitContext.putRouteTag(TencentUnitContext.CLOUD_SPACE_ROUTE_GATEWAY, gateway);

		TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SERVICE, serviceName);
		TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_CLOUD, TencentUnitManager.getUnitCloudMap()
				.get(unitId));
	}

	private static Gateway getGateway(String unitId) {
		return getGateway(unitId, TencentUnitContext.getSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM));
	}

	private static Gateway getGateway(String unitId, String system) {
		String targetGatewayCloudId = TencentUnitManager.getUnitCloudMap().get(unitId);
		if (!EnvironmentUtils.isGateway()) {
			targetGatewayCloudId = TencentUnitManager.getLocalCloudSpaceId();
		}
		Gateway result = TencentUnitManager.getBusinessSystemGateway(targetGatewayCloudId, system);
		if (result == null) {
			String msg = String.format("[getGateway] gateway not exist, targetGatewayCloudId:%s, unitId:%s, system:%s", targetGatewayCloudId, unitId, system);
			LOGGER.error(msg);
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, msg);
		}
		return result;
	}
}
