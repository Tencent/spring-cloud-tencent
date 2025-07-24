/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitContext.UnitCompositeContextMap;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.UnitTag;
import com.tencent.tsf.unit.core.UnitTagConstant;
import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;
import com.tencent.tsf.unit.core.model.RoutingUnit;
import com.tencent.tsf.unit.core.model.RoutingUnitContext;
import com.tencent.tsf.unit.core.model.TargetUnitInfo;
import com.tencent.tsf.unit.core.model.UnitArch;
import com.tencent.tsf.unit.core.model.UnitInfo;
import com.tencent.tsf.unit.core.model.UnitNamespace;
import com.tencent.tsf.unit.core.model.UnitRouteInfo;
import com.tencent.tsf.unit.core.model.UnitTagPosition;
import com.tencent.tsf.unit.core.model.UnitType;
import com.tencent.tsf.unit.core.remote.TsfUnitConsulManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 该类提供给客户使用，客户不直接使用 TencentUnitManager，后续的迭代只需要这里保持兼容性即可.
 */
public final class TencentUnitUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TencentUnitUtils.class);

	private TencentUnitUtils() {
	}

	/**
	 * 根据传入的客户要素（customerIdentifier），判断是否和本地的所在单元是同一个单元.
	 */
	public static boolean checkLocalUnit(String customerIdentifier) {
		RoutingUnit routingUnit = TencentUnitManager.getRoutingUnit(customerIdentifier);
		if (routingUnit != null) {
			return StringUtils.equals(routingUnit.getUnitId(), TencentUnitManager.getLocalUnitId());
		}
		else {
			return false;
		}
	}

	/**
	 * 传入 unitId（单元号），判断是否和该单元号处在同一个 cloud space 里.
	 */
	public static boolean checkLocalCloudSpace(String unitId) {
		String targetCloudId = TencentUnitManager.getUnitCloudMap().get(unitId);
		return StringUtils.equals(targetCloudId, TencentUnitManager.getLocalCloudSpaceId());
	}


	/**
	 * 获取本地对应的SDU信息，若在GDU上执行，返回 null，需适配灰度.
	 *
	 * @return UnitInfo 单元信息
	 */
	public static UnitInfo getLocalStandardUnitInfo() {
		// 判断是否在 gdu 上
		if (TencentUnitManager.getGduNsMap().containsKey(TencentUnitManager.getLocalUnitId())
				|| TencentUnitManager.getGrayGduNsMap().containsKey(TencentUnitManager.getLocalUnitId())) {
			return null;
		}

		return getStandardUnitInfo(TencentUnitManager.getLocalUnitId());
	}

	/**
	 * 获取本Region下对应的GDU，需要适配灰度.
	 *
	 * @return 单元信息
	 */
	public static UnitInfo getLocalGlobalUnitInfo() {
		UnitInfo info = new UnitInfo();

		UnitArch.CloudSpace cloudSpace = TencentUnitManager.getLocalCloudSpace();
		if (cloudSpace != null) {
			info.setRegionId(cloudSpace.getRegionId());
			info.setRegionName(cloudSpace.getRegionName());
			List<UnitArch.Gdu> gduList;
			if (TencentUnitManager.inGrayUnit()) {
				gduList = cloudSpace.getGrayGdus();
			}
			else {
				gduList = cloudSpace.getGdus();
			}
			// 默认就是只有一个GDU
			for (UnitArch.Gdu gdu : gduList) {
				String failoverUnitId = TencentUnitManager.getFailoverUnitIdMap().get(gdu.getId());
				// 如果有容灾单元，则使用容灾单元
				if (StringUtils.isNotEmpty(failoverUnitId)
						&& TencentUnitManager.getAllGduMap().containsKey(failoverUnitId)) {
					gdu = TencentUnitManager.getAllGduMap().get(failoverUnitId);
				}
				info.setUnitId(gdu.getId());
				info.setUnitType(UnitType.GDU.toString());
				info.setZoneId(gdu.getZoneId());
				info.setZoneName(gdu.getZoneName());
				info.setAllNamespaceList(gdu.getNamespaces());
				info.setAllShardingKeyList(Collections.emptyList());
				return info;
			}
		}
		return null;
	}

	/**
	 * 根据目标业务系统，目标客户要素，获取目标SDU信息，需适配灰度，需适配容灾.
	 *
	 * @param systemName 目标业务系统
	 * @param cid        目标客户要素
	 * @return 目标单元信息
	 */
	public static TargetUnitInfo getTargetStandardUnitInfo(String systemName, String cid) {
		UnitInfo unitInfo = null;
		TargetUnitInfo targetUnitInfo = null;
		String customerNumber = null;
		// 在灰度单元内
		if (TencentUnitManager.inGrayUnit()) {
			unitInfo = getStandardUnitInfo(TencentUnitManager.getGraySduUnitId());
			targetUnitInfo = new TargetUnitInfo(unitInfo);
			// 灰度不需要计算映射值
		}
		else {
			RoutingUnit routingUnit = TencentUnitManager.getRoutingUnit(cid);
			if (routingUnit == null) {
				LOGGER.error("[unit] cid: {}  not get any match unit route", cid);
				throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, String.format("system: %s, cid: %s not found match unit route", systemName, cid));
			}
			customerNumber = routingUnit.getCustomerNumber();
			unitInfo = getStandardUnitInfo(routingUnit.getUnitId());
			targetUnitInfo = new TargetUnitInfo(unitInfo);
			// 设置计算转换后的映射值
			targetUnitInfo.setShardingKey(routingUnit.getShardingKey());
		}

		// 找到目标Namespace信息
		for (UnitNamespace namespace : targetUnitInfo.getAllNamespaceList()) {
			if (namespace.getBusinessSystemName().equals(systemName)) {
				targetUnitInfo.setNamespaceId(namespace.getId());
				targetUnitInfo.setNamespaceName(namespace.getName());
				return targetUnitInfo;
			}
		}

		LOGGER.error("[unit] cid: {}, customerNumber: {}, system: {}, not found target namespace",
				cid, customerNumber, systemName);
		throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, String.format("system: %s, cid: %s not found target unit namespace", systemName, cid));
	}


	/**
	 * 返回所有地域所有的SDU，.
	 * from 2025-01-17 需适配灰度
	 * 需要适配容灾（不返回故障单元，故障单元的分配挪到对应的容灾单元）
	 * 使用场景: 在消息队列发送消息时用到的，需要广播消息到所有活跃中的SDU
	 */
	public static List<UnitInfo> getAllStandardUnitInfoList() {

		// 当前是否处于灰度单元内
		if (TencentUnitManager.inGrayUnit()) {
			return getGraySduUnitInfos();
		}
		else {
			return getSduUnitInfos();
		}
	}


	/**
	 * 获取路由上下文传递过来的单元化信息.
	 */
	public static RoutingUnitContext getRoutingUnitContext() {
		String customerNumber = TencentUnitContext.getSourceTag(TencentUnitContext.CLOUD_SPACE_TARGET_CUSTOMER_NUMBER);
		String shardingKey = TencentUnitContext.getSourceTag(TencentUnitContext.CLOUD_SPACE_TARGET_SHARDING_KEY);
		String unitId = TencentUnitContext.getSourceTag(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID);

		return new RoutingUnitContext(customerNumber, shardingKey, unitId);
	}

	/**
	 * 设置业务的灰度标识。position为灰度标识的位置，只支持传递HEADER，tags为标识组合.
	 */
	public static void putGrayTags(String position, Map<String, String> tags) {
		UnitTagPosition unitTagPosition = UnitTagPosition.fromString(position);
		if (unitTagPosition == null) {
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, "gray tag position format error, pos:" + position);
		}
		TencentUnitContext.clearGrayUserContext();
		switch (unitTagPosition) {
		case HEADER:
			break;
		default:
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, "gray tag position only support header, pos:" + position);
		}
		TencentUnitContext.putGrayUserTags(position, tags);
	}


	public static void putCustomerTags(String system, String cid) {
		TencentUnitContext.clearUserTags();

		TencentUnitContext.putUserTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM, system);
		TencentUnitContext.putUserTag(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER, cid);
	}

	/**
	 * context写入unit相关数据.
	 *
	 * @param system 业务系统
	 * @param cid    客户要素
	 * @param global 是否只转给GDU
	 */
	public static void putCustomerTags(String system, String cid, boolean global) {
		putCustomerTags(system, cid);
		if (global) {
			TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GDU_FORWARD_ONLY, Boolean.TRUE.toString());
		}
	}

	/**
	 * 往context里写入目标业务系统+目标单元号.
	 */
	public static void putTargetUnitTags(String system, String unitId) {
		TencentUnitContext.clearUserTags();

		TencentUnitContext.putUserTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM, system);
		TencentUnitContext.putUserTag(TencentUnitContext.CLOUD_SPACE_TARGET_UNIT_ID, unitId);
	}

	/**
	 * tsf spring cloud 普通应用通过注解开启，网关默认都开启.
	 */
	public static void enable() {
		TencentUnitManager.setEnable(true);
		TsfUnitConsulManager.init();
	}

	public static UnitCompositeContextMap getCompositeContextMap() {
		return TencentUnitContext.getCompositeContextMap();
	}

	public static void setUnitCompositeContextMap(UnitCompositeContextMap unitCompositeContextMap) {
		TencentUnitContext.setUnitCompositeContextMap(unitCompositeContextMap);
	}

	public static List<Integer> getALLSharding() {
		List<UnitRouteInfo.UnitRoute> unitRoutes = TencentUnitManager.getUnitRoutes();
		if (unitRoutes.size() == 0) {
			return Collections.emptyList();
		}
		List<Integer> shardingIdList = new ArrayList<>();
		for (UnitRouteInfo.UnitRoute unitRoute : unitRoutes) {
			appendShardingIdList(unitRoute, shardingIdList);
		}
		shardingIdList.sort(Comparator.naturalOrder());

		return shardingIdList;
	}

	private static void appendShardingIdList(UnitRouteInfo.UnitRoute unitRoute, List<Integer> shardingIdList) {
		// 单元化路径规则下仅支持识别操作符：range，equal，in
		List<UnitTag> unitTagList = unitRoute.getMatch().getConditions();
		for (UnitTag unitTag : unitTagList) {
			if (unitTag.getTagOperator().equals(UnitTagConstant.OPERATOR.RANGE)) {
				String[] values = unitTag.getTagValue().split("\\s*,\\s*");
				if (values.length != 2) {
					LOGGER.warn("[unit] unit rule tag value: {} is invalid", unitTag.getTagValue());
					continue;
				}

				// 转换为int进行range匹配
				int start = Integer.parseInt(values[0]);
				int end = Integer.parseInt(values[1]);
				for (int i = start; i <= end; i++) {
					shardingIdList.add(i);
				}
			}
			else if (unitTag.getTagOperator().equals(UnitTagConstant.OPERATOR.EQUAL)) {
				int target = Integer.parseInt(unitTag.getTagValue());
				shardingIdList.add(target);
			}
			else if (unitTag.getTagOperator().equals(UnitTagConstant.OPERATOR.IN)) {
				String[] values = unitTag.getTagValue().split("\\s*,\\s*");
				for (String entry : values) {
					int target = Integer.parseInt(entry);
					shardingIdList.add(target);
				}
			}
		}
	}


	/**
	 * 通过解析单元化架构配置，返回对应单元号的SDU.
	 * private 方法，不对外暴露
	 *
	 * @param unitId 单元号
	 * @return UnitInfo 单元信息
	 */
	private static UnitInfo getStandardUnitInfo(String unitId) {
		if (StringUtils.isEmpty(unitId)) {
			LOGGER.error("[unit] not found sdu local id");
			throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, "unit id is empty");
		}

		UnitInfo info = new UnitInfo();
		info.setUnitId(unitId);

		// 填充单元信息，包括suds，namespaces等
		fillStandardUnitInfo(info);

		// 增加SharingList信息
		appendStandardUnitShardingList(info);

		return info;
	}

	private static void fillStandardUnitInfo(UnitInfo info) {
		UnitArch.CloudSpace cloudSpace = TencentUnitManager.getLocalCloudSpace();
		info.setRegionId(cloudSpace.getRegionId());
		info.setRegionName(cloudSpace.getRegionName());
		List<UnitArch.Sdu> sduList;
		if (TencentUnitManager.inGrayUnit()) {
			sduList = cloudSpace.getGraySdus();
		}
		else {
			sduList = cloudSpace.getSdus();
		}
		for (UnitArch.Sdu sdu : sduList) {
			if (!sdu.getId().equals(info.getUnitId())) {
				continue;
			}

			info.setUnitType(UnitType.SDU.toString());
			info.setZoneId(sdu.getZoneId());
			info.setZoneName(sdu.getZoneName());
			info.setAllNamespaceList(sdu.getNamespaces());
			return;
		}

		LOGGER.error("[unit] unitId: {}, cloudSpace: {}, not found unit in sdu list",
				info.getUnitId(), cloudSpace.getCloudId());
		throw new TencentUnitException(ErrorCode.COMMON_PARAMETER_ERROR, String.format("local unitId: %s, cloudSpace: %s, not found in the sdu list",
				info.getUnitId(), cloudSpace.getCloudId()));
	}

	/**
	 * 给定单元，返回路由规则里对应的shardingKeyList.
	 */
	private static void appendStandardUnitShardingList(UnitInfo info) {
		List<UnitRouteInfo.UnitRoute> unitRoutes = TencentUnitManager.getUnitRoutes();
		if (unitRoutes.size() == 0) {
			LOGGER.warn("[unit] unit routes size is empty, so shardingList is empty");
			return;
		}

		List<Integer> shardingIdList = new ArrayList<>();
		for (UnitRouteInfo.UnitRoute unitRoute : unitRoutes) {
			if (!unitRoute.getRoute().getActualUnitId().equals(info.getUnitId())) {
				continue;
			}

			appendShardingIdList(unitRoute, shardingIdList);
		}
		// 容灾场景下会合并多个单元的 shardingKeyList, 这里需要排序
		shardingIdList.sort(Comparator.naturalOrder());
		info.setAllShardingKeyList(shardingIdList);
	}


	private static List<UnitInfo> getSduUnitInfos() {
		List<UnitInfo> infos = new ArrayList<>();
		for (UnitArch.CloudSpace cloudSpace : TencentUnitManager.getAllCloudSpaces()) {
			if (cloudSpace.getSdus() == null) {
				continue;
			}
			for (UnitArch.Sdu sdu : cloudSpace.getSdus()) {
				// 过滤非活跃单元（故障单元、未配置路由单元）
				if (!TencentUnitManager.getActiveSduIdMap().containsKey(sdu.getId())) {
					continue;
				}
				infos.add(getUnitInfo(sdu, cloudSpace));
			}
		}
		return infos;
	}

	private static List<UnitInfo> getGraySduUnitInfos() {
		List<UnitInfo> infos = new ArrayList<>();
		for (UnitArch.CloudSpace cloudSpace : TencentUnitManager.getAllCloudSpaces()) {
			if (cloudSpace.getGraySdus() == null) {
				continue;
			}
			for (UnitArch.Sdu sdu : cloudSpace.getGraySdus()) {
				infos.add(getUnitInfo(sdu, cloudSpace));
			}
		}
		return infos;
	}

	private static UnitInfo getUnitInfo(UnitArch.Sdu sdu, UnitArch.CloudSpace cloudSpace) {
		UnitInfo info = new UnitInfo();
		// 单元基础信息
		info.setUnitId(sdu.getId());
		info.setUnitType(UnitType.SDU.toString());
		info.setZoneId(sdu.getZoneId());
		info.setZoneName(sdu.getZoneName());
		info.setAllNamespaceList(sdu.getNamespaces());
		// 增加Region
		info.setRegionId(cloudSpace.getRegionId());
		info.setRegionName(cloudSpace.getRegionName());
		// 增加SharingList信息
		appendStandardUnitShardingList(info);
		return info;
	}
}
