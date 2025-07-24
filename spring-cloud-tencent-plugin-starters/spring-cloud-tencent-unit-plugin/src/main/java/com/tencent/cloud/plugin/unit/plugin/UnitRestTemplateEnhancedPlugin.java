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

import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.tsf.unit.core.TencentUnitContext;
import shade.polaris.com.google.common.collect.ImmutableMap;

import org.springframework.http.HttpRequest;

/**
 * Pre EnhancedPlugin for rest template to encode unit metadata.
 *
 * @author Shedfree Wu
 */
public class UnitRestTemplateEnhancedPlugin implements EnhancedPlugin {
	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.BEFORE_CALLING;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof HttpRequest httpRequest)) {
			return;
		}

		TencentUnitContext.UnitCompositeContextMap unitCompositeContextMap = TencentUnitContext.getCompositeContextMap();
		if (!CollectionUtils.isEmpty(unitCompositeContextMap.getSystemContext())) {
			buildMetadataHeader(httpRequest, unitCompositeContextMap.getSystemContext(), MetadataConstant.HeaderName.TSF_UNIT);
		}
	}

	private void buildHeaderMap(HttpRequest request, Map<String, String> headerMap) {
		if (!CollectionUtils.isEmpty(headerMap)) {
			headerMap.forEach((key, value) -> request.getHeaders().set(key, UrlUtils.encode(value)));
		}
	}

	/**
	 * Set metadata into the request header for {@link HttpRequest} .
	 *
	 * @param request    instance of {@link HttpRequest}
	 * @param metadata   metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(HttpRequest request, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			buildHeaderMap(request, ImmutableMap.of(headerName, JacksonUtils.serialize2Json(metadata)));
		}
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.CONSUMER_UNIT_METADATA_PLUGIN_ORDER;
	}
}
