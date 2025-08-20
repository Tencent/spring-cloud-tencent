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

package com.tencent.cloud.plugin.trace.attribute.tsf;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.MetadataContextUtils;
import com.tencent.cloud.common.util.OtUtils;
import com.tencent.cloud.plugin.trace.attribute.SpanAttributesProvider;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.MetadataObjectValue;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.router.lane.LaneRouter;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.tsf.core.entity.Tag;

public class TsfSpanAttributesProvider implements SpanAttributesProvider {

	@Override
	public Map<String, String> getClientBaggageAttributes(EnhancedPluginContext context) {
		Map<String, String> attributes = new HashMap<>();
		if (context.getRequest() != null && StringUtils.isNotBlank(context.getRequest().getPath())) {
			attributes.put("remoteInterface", context.getRequest().getPath());
		}
		ServiceInstance targetServiceInstance = context.getTargetServiceInstance();
		if (null != targetServiceInstance && CollectionUtils.isNotEmpty(targetServiceInstance.getMetadata())) {
			if (targetServiceInstance.getMetadata().containsKey(TsfMetadataConstants.TSF_NAMESPACE_ID)) {
				attributes.put("remote.namespace-id", StringUtils.defaultString(
						targetServiceInstance.getMetadata().get(TsfMetadataConstants.TSF_NAMESPACE_ID)));
			}
			if (targetServiceInstance.getMetadata().containsKey(TsfMetadataConstants.TSF_GROUP_ID)) {
				attributes.put("remote.group-id", StringUtils.defaultString(
						targetServiceInstance.getMetadata().get(TsfMetadataConstants.TSF_GROUP_ID)));
			}
			if (targetServiceInstance.getMetadata().containsKey(TsfMetadataConstants.TSF_APPLICATION_ID)) {
				attributes.put("remote.application-id", StringUtils.defaultString(
						targetServiceInstance.getMetadata().get(TsfMetadataConstants.TSF_APPLICATION_ID)));
			}
		}
		if (StringUtils.isBlank(attributes.get("remote.namespace-id"))) {
			attributes.put("remote.namespace-id", context.getRequest().getGovernanceNamespace());
		}

		MetadataObjectValue<Tag> langTagObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.CUSTOM, false).
				getMetadataValue(ContextConstant.LANE_TAG);
		if (MetadataContextUtils.existMetadataValue(langTagObject)) {
			attributes.put(OtUtils.OTEL_LANE_ID_KEY, langTagObject.getObjectValue().get().getValue());
		}

		MetadataObjectValue<Map<String, String>> extraTraceAttributeObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.CUSTOM, false).
				getMetadataValue(ContextConstant.Trace.EXTRA_TRACE_ATTRIBUTES);
		if (MetadataContextUtils.existMetadataValue(extraTraceAttributeObject)) {
			Map<String, String> extraTraceAttributes = extraTraceAttributeObject.getObjectValue().get();
			attributes.putAll(extraTraceAttributes);
		}
		return attributes;
	}

	@Override
	public Map<String, String> getServerPreSpanAttributes(EnhancedPluginContext context) {
		Map<String, String> attributes = new HashMap<>();

		MetadataContext metadataContext = MetadataContextHolder.get();
		Map<String, String> upstreamDisposableCustomAttributes = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE);
		if (CollectionUtils.isNotEmpty(upstreamDisposableCustomAttributes)) {
			for (Map.Entry<String, String> entry : upstreamDisposableCustomAttributes.entrySet()) {
				if (LaneRouter.TRAFFIC_STAIN_LABEL.equals(entry.getKey()) && entry.getValue().startsWith("tsf/")) {
					attributes.put(OtUtils.OTEL_LANE_ID_KEY, entry.getValue().split("/")[1]);
				}
			}
		}
		return attributes;
	}

	@Override
	public Map<String, String> getServerFinallySpanAttributes(EnhancedPluginContext context) {
		Map<String, String> attributes = new HashMap<>();
		MetadataObjectValue<Map<String, String>> extraTraceAttributeObject = MetadataContextHolder.get().
				getMetadataContainer(MetadataType.CUSTOM, false).
				getMetadataValue(ContextConstant.Trace.EXTRA_TRACE_ATTRIBUTES);
		if (MetadataContextUtils.existMetadataValue(extraTraceAttributeObject)) {
			Map<String, String> extraTraceAttributes = extraTraceAttributeObject.getObjectValue().get();
			attributes.putAll(extraTraceAttributes);
		}
		return attributes;
	}
}
