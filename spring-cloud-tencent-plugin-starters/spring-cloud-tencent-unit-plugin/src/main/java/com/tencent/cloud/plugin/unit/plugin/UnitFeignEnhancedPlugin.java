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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant;
import com.tencent.tsf.unit.core.TencentUnitContext;
import feign.Request;
import shade.polaris.com.google.common.collect.ImmutableMap;

import org.springframework.util.CollectionUtils;

/**
 * Pre EnhancedPlugin for feign to encode unit metadata.
 *
 * @author Shedfree Wu
 */
public class UnitFeignEnhancedPlugin implements EnhancedPlugin {
	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.BEFORE_CALLING;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!(context.getOriginRequest() instanceof Request request)) {
			return;
		}

		TencentUnitContext.UnitCompositeContextMap unitCompositeContextMap = TencentUnitContext.getCompositeContextMap();
		if (!com.tencent.polaris.api.utils.CollectionUtils.isEmpty(unitCompositeContextMap.getSystemContext())) {
			buildMetadataHeader(request, unitCompositeContextMap.getSystemContext(), MetadataConstant.HeaderName.TSF_UNIT);
		}
	}

	/**
	 * Set metadata into the request header for {@link Request} .
	 * @param request instance of {@link Request}
	 * @param metadata metadata map .
	 * @param headerName target metadata http header name .
	 */
	private void buildMetadataHeader(Request request, Map<String, String> metadata, String headerName) {
		if (!CollectionUtils.isEmpty(metadata)) {
			buildHeaderMap(request, ImmutableMap.of(headerName, JacksonUtils.serialize2Json(metadata)));
		}
	}


	/**
	 * Set headerMap into the request header for {@link Request} .
	 * @param request instance of {@link Request}
	 * @param headerMap header map .
	 */
	private void buildHeaderMap(Request request, Map<String, String> headerMap) {
		if (!CollectionUtils.isEmpty(headerMap)) {
			Map<String, Collection<String>> headers = getModifiableHeaders(request);
			headerMap.forEach((key, value) -> headers.put(key, Collections.singletonList(UrlUtils.encode(value))));
		}
	}

	/**
	 * The value obtained directly from the headers method is an unmodifiable map.
	 * If the Feign client uses the URL, the original headers are unmodifiable.
	 * @param request feign request
	 * @return modifiable headers
	 */
	private Map<String, Collection<String>> getModifiableHeaders(Request request) {
		Map<String, Collection<String>> headers;
		headers = (Map<String, Collection<String>>) ReflectionUtils.getFieldValue(request, "headers");

		if (!(headers instanceof LinkedHashMap)) {
			headers = new LinkedHashMap<>(headers);
			ReflectionUtils.setFieldValue(request, "headers", headers);
		}
		return headers;
	}

	@Override
	public int getOrder() {
		return PluginOrderConstant.ClientPluginOrder.CONSUMER_UNIT_METADATA_PLUGIN_ORDER;
	}
}
