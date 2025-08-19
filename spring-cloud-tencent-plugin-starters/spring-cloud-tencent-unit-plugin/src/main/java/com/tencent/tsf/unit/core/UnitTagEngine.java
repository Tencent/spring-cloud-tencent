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

package com.tencent.tsf.unit.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.UnitTagConstant.TagRuleRelation;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.GrayUnitRoute;
import com.tencent.tsf.unit.core.model.UnitRouteInfo.UnitRoute;

public final class UnitTagEngine {

	private UnitTagEngine() {
	}

	private static final Map<String, Pattern> PATTERN_CACHE_MAP = new ConcurrentHashMap<>();

	public static boolean matchTag(UnitTag tag, String targetTagValue) {
		String tagField = tag.getTagField();
		String tagOperator = tag.getTagOperator();
		String tagValue = tag.getTagValue();


		if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.EQUAL)) {
			// 匹配关系  等于
			return StringUtils.equals(tagValue, targetTagValue);
		}
		else if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.NOT_EQUAL)) {
			// 匹配关系  不等于
			return !StringUtils.equals(tagValue, targetTagValue);
		}
		else if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.IN)) {
			// 匹配关系  包含
			Set<String> routeTagValueSet = new HashSet<>(Arrays.asList(tagValue.split("\\s*,\\s*")));
			return routeTagValueSet.contains(targetTagValue);
		}
		else if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.NOT_IN)) {
			// 匹配关系  不包含
			Set<String> routeTagValueSet = new HashSet<>(Arrays.asList(tagValue.split("\\s*,\\s*")));
			return !routeTagValueSet.contains(targetTagValue);
		}
		else if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.REGEX)) {
			// 如果没有 targetTagValue，认为不匹配
			if (targetTagValue == null) {
				return false;
			}
			// 匹配关系  正则
			Pattern pattern = PATTERN_CACHE_MAP.get(tagValue);
			if (pattern == null) {
				pattern = Pattern.compile(tagValue);
				PATTERN_CACHE_MAP.putIfAbsent(targetTagValue, pattern);
			}

			return pattern.matcher(targetTagValue).matches();
		}
		else if (StringUtils.equals(tagOperator, UnitTagConstant.OPERATOR.RANGE)) {
			// 按,分割tagValue
			String[] values = tagValue.split("\\s*,\\s*");
			if (values.length != 2) {
				return false;
			}

			try {
				// 转换为int进行range匹配
				int start = Integer.parseInt(values[0]);
				int end = Integer.parseInt(values[1]);
				int target = Integer.parseInt(targetTagValue);
				if (target >= start && target <= end) {
					return true;
				}
			}
			catch (NumberFormatException ex) {
				return false;
			}

			return false;
		}
		else {
			return false;
		}
	}

	/**
	 * 检查传入的 route 是否命中.
	 */
	public static boolean checkUnitRouteHit(UnitRoute route, Map<String, String> targetTagMap) {
		// condition 之间是 or 关系
		if (StringUtils.equalsIgnoreCase(TagRuleRelation.OR, route.getMatch().getOperator())) {
			for (UnitTag condition : route.getMatch().getConditions()) {
				// 有一组匹配直接返回成功
				if (UnitTagEngine.matchTag(condition, targetTagMap.get(condition.getTagField()))) {
					return true;
				}
			}
		}
		else {
			// and 关系
			for (UnitTag condition : route.getMatch().getConditions()) {
				// 有一组不匹配则直接返回 null
				if (!UnitTagEngine.matchTag(condition, targetTagMap.get(condition.getTagField()))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static boolean checkGrayUnitRouteHit(GrayUnitRoute route) {
		// condition 之间是 or 关系
		if (StringUtils.equalsIgnoreCase(TagRuleRelation.OR, route.getMatch().getOperator())) {
			for (UnitTag condition : route.getMatch().getConditions()) {
				// 有一组匹配直接返回成功
				if (UnitTagEngine.matchTag(condition,
						TencentUnitContext.getGrayTag(condition.getTagPosition().name(), condition.getTagField()))) {
					return true;
				}
			}
			return false;
		}
		else {
			// and 关系
			for (UnitTag condition : route.getMatch().getConditions()) {
				// 有一组不匹配则直接返回 null
				if (!UnitTagEngine.matchTag(condition,
						TencentUnitContext.getGrayTag(condition.getTagPosition().name(), condition.getTagField()))) {
					return false;
				}
			}
			return true;
		}
	}

}
