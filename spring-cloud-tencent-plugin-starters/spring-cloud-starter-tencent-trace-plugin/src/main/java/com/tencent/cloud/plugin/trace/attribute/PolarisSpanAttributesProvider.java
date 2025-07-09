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

package com.tencent.cloud.plugin.trace.attribute;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;

import static com.tencent.cloud.common.util.OtUtils.OTEL_LANE_ID_KEY;
import static com.tencent.polaris.plugins.router.lane.LaneRouter.TRAFFIC_STAIN_LABEL;

/**
 * Implementation of {@link SpanAttributesProvider} for polaris.
 *
 * @author Haotian Zhang
 */
public class PolarisSpanAttributesProvider implements SpanAttributesProvider {
	@Override
	public Map<String, String> getServerSpanAttributes(EnhancedPluginContext context) {
		Map<String, String> attributes = new HashMap<>();
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> transitiveCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		if (CollectionUtils.isNotEmpty(transitiveCustomAttributes)) {
			for (Map.Entry<String, String> entry : transitiveCustomAttributes.entrySet()) {
				attributes.put("custom." + entry.getKey(), entry.getValue());
			}
		}
		Map<String, String> disposableCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE);
		if (CollectionUtils.isNotEmpty(disposableCustomAttributes)) {
			for (Map.Entry<String, String> entry : disposableCustomAttributes.entrySet()) {
				attributes.put("custom." + entry.getKey(), entry.getValue());
			}
		}
		Map<String, String> upstreamDisposableCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE);
		if (CollectionUtils.isNotEmpty(upstreamDisposableCustomAttributes)) {
			for (Map.Entry<String, String> entry : upstreamDisposableCustomAttributes.entrySet()) {
				attributes.put("custom." + entry.getKey(), entry.getValue());
				if (MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME.equals(entry.getKey())) {
					attributes.put("net.peer.service", entry.getValue());
				}
			}
		}
		attributes.put("http.port", CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.getRawMetadataStringValue(MetadataConstants.LOCAL_PORT));
		return attributes;
	}

	@Override
	public Map<String, String> getClientBaggageAttributes(EnhancedPluginContext context) {
		Map<String, String> attributes = new HashMap<>();
		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> transitiveCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		if (CollectionUtils.isNotEmpty(transitiveCustomAttributes)) {
			for (Map.Entry<String, String> entry : transitiveCustomAttributes.entrySet()) {
				attributes.put("custom." + entry.getKey(), entry.getValue());
			}
		}
		Map<String, String> disposableCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE);
		if (CollectionUtils.isNotEmpty(disposableCustomAttributes)) {
			for (Map.Entry<String, String> entry : disposableCustomAttributes.entrySet()) {
				attributes.put("custom." + entry.getKey(), entry.getValue());
			}
		}
		attributes.put("http.port", CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.getRawMetadataStringValue(MetadataConstants.LOCAL_PORT));
		attributes.put("net.peer.service", context.getRequest().getHost());

		String serviceLane = metadataContext.getMetadataContainer(MetadataType.MESSAGE, false)
				.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, TRAFFIC_STAIN_LABEL);
		if (StringUtils.isNotBlank(serviceLane)) {
			String[] splits = StringUtils.split(serviceLane, "/");
			if (splits.length >= 2) {
				attributes.put(OTEL_LANE_ID_KEY, splits[1]);
			}
		}
		return attributes;
	}
}
