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

package com.tencent.cloud.plugin.unit.plugin;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.model.UnitTagPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitServletPreEnhancedPlugin implements EnhancedPlugin {
	private static final Logger logger = LoggerFactory.getLogger(UnitServletPreEnhancedPlugin.class);

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Server.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof HttpServletRequest)) {
			return;
		}

		HttpServletRequest httpServletRequest = (HttpServletRequest) context.getOriginRequest();

		if (!TencentUnitManager.isEnable()) {
			return;
		}

		TencentUnitContext.removeAll();

		String unitContextEncoded = httpServletRequest.getHeader(MetadataConstant.HeaderName.TSF_UNIT);
		if (StringUtils.isNotEmpty(unitContextEncoded)) {
			Map<String, String> unitMap = JacksonUtils.deserialize2Map(URLDecoder.decode(unitContextEncoded, StandardCharsets.UTF_8.name()));

			TencentUnitContext.putSourceTags(unitMap);
			if (TencentUnitManager.isContextPassingThroughEnabled()) {
				// 不能传递所有的 key, 只需要传递客户要素和业务系统,灰度信息
				Optional.ofNullable(unitMap.get(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER)).
						ifPresent(value -> TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_CUSTOMER_IDENTIFIER, value));
				Optional.ofNullable(unitMap.get(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM)).
						ifPresent(value -> TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_TARGET_SYSTEM, value));
				Optional.ofNullable(unitMap.get(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO)).
						ifPresent(value -> TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO, value));
			}
			else {
				// 没有 passing through 标志，灰度信息也要透传
				Optional.ofNullable(unitMap.get(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO)).
						ifPresent(value -> TencentUnitContext.putSystemTag(TencentUnitContext.CLOUD_SPACE_GRAY_UNIT_INFO, value));
			}
		}

		for (String grayKey : TencentUnitManager.getGrayUnitHeaderKey()) {
			String value = httpServletRequest.getHeader(grayKey);
			// 非空 value 才设置，便于匹配灰度规则时能否直接跳过
			if (StringUtils.isNotEmpty(value)) {
				TencentUnitContext.putGrayUserTag(UnitTagPosition.HEADER.name(), grayKey, value);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[getSerializeTagsFromRequestMeta] unit context:{}",
					TencentUnitContext.getOriginCompositeContextMap());
		}

	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ServerPluginOrder.TRACE_SERVER_PRE_PLUGIN_ORDER;
	}
}
