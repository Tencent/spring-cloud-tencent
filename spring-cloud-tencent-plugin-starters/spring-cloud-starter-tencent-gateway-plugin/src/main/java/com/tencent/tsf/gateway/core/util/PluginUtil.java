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

package com.tencent.tsf.gateway.core.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.gateway.context.Position;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.plugins.connector.consul.service.common.TagCondition;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConditionUtil;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.constant.PluginConstants;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.model.PluginDetail;
import com.tencent.tsf.gateway.core.model.PluginInfo;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import com.tencent.tsf.gateway.core.model.RequestTransformerPluginInfo;
import com.tencent.tsf.gateway.core.model.TagPluginInfo;
import com.tencent.tsf.gateway.core.model.TransformerAction;
import com.tencent.tsf.gateway.core.model.TransformerTag;
import io.opentelemetry.api.trace.Span;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.com.google.common.base.Joiner;
import tools.jackson.core.type.TypeReference;

import org.springframework.tsf.core.TsfContext;
import org.springframework.tsf.core.entity.Tag;
import org.springframework.util.AntPathMatcher;

public final class PluginUtil {

	private static final Logger logger = LoggerFactory.getLogger(PluginUtil.class);

	private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

	private PluginUtil() {

	}

	public static List<PluginDetail> sortPluginDetail(Stream<PluginDetail> pluginDetailStream) {
		return pluginDetailStream.distinct().sorted(Comparator.comparing(PluginInfo::getOrder)).collect(
				Collectors.toList());
	}

	public static List<TagPluginInfo> deserializeTagPluginInfoList(String tagPluginInfoListJson) {
		if (StringUtils.length(tagPluginInfoListJson) == 0) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_REQUIRED, tagPluginInfoListJson);
		}
		List<TagPluginInfo> tagPluginInfoList = null;
		try {
			tagPluginInfoList = JacksonUtils.deserialize(tagPluginInfoListJson, new TypeReference<List<TagPluginInfo>>() { });
		}
		catch (Throwable t) {
			logger.error("deserialize tagPluginInfoList : {} occur exception : {}", tagPluginInfoListJson, t);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_PARAMETER_INVALID, tagPluginInfoListJson);
		}
		return tagPluginInfoList;
	}

	public static PluginPayload transferToTag(String tagPluginListJson, TsfGatewayRequest tsfGatewayRequest,
			PluginPayload payload) {
		List<TagPluginInfo> tagPluginInfoList = PluginUtil.deserializeTagPluginInfoList(tagPluginListJson);
		if (CollectionUtils.isEmpty(tagPluginInfoList)) {
			return payload;
		}
		Map<String, String> responseHeader = new HashMap<>();
		Map<String, String> tagMap = new HashMap<>(tagPluginInfoList.size());

		for (TagPluginInfo tagPluginInfo : tagPluginInfoList) {
			Position tagPosition = tagPluginInfo.getTagPosition();
			String preTagName = tagPluginInfo.getPreTagName();
			String postTagName = tagPluginInfo.getPostTagName();
			String traceIdEnabled = tagPluginInfo.getTraceIdEnabled();
			switch (tagPosition) {
			case QUERY:
				Map<String, String[]> parameterMap = tsfGatewayRequest.getParameterMap();
				String[] queryTags = parameterMap.get(preTagName);
				if (queryTags != null) {
					if (StringUtils.isBlank(postTagName)) {
						postTagName = preTagName;
					}
					String tagValue = String.join(",", queryTags);
					tagMap.put(postTagName, tagValue);
					relateTraceId(responseHeader, postTagName, traceIdEnabled, tagValue);
				}
				break;
			case COOKIE:
				String cookieTag = tsfGatewayRequest.getCookie(preTagName);
				if (cookieTag != null) {
					if (StringUtils.isBlank(postTagName)) {
						postTagName = preTagName;
					}
					tagMap.put(postTagName, cookieTag);
					relateTraceId(responseHeader, postTagName, traceIdEnabled, cookieTag);
				}
				break;
			case HEADER:
				String headerTag = tsfGatewayRequest.getRequestHeader(preTagName);
				if (headerTag != null) {
					if (StringUtils.isBlank(postTagName)) {
						postTagName = preTagName;
					}
					tagMap.put(postTagName, headerTag);
					relateTraceId(responseHeader, postTagName, traceIdEnabled, headerTag);
				}
				break;
			case PATH:
				String path = tsfGatewayRequest.getUri().getPath();
				if (StringUtils.isNotBlank(path)) {
					// ensure path start with '/'
					if (path.charAt(0) != '/') {
						path = "/" + path;
					}
					// apiPath is the path of downstream service.
					String apiPath = path;
					boolean matched = antPathMatcher.match(preTagName, apiPath);
					if (matched) {
						Map<String, String> pathTagMap = antPathMatcher.extractUriTemplateVariables(preTagName, apiPath);
						if (CollectionUtils.isNotEmpty(pathTagMap)) {
							for (Map.Entry<String, String> entry : pathTagMap.entrySet()) {
								String pathTag = entry.getValue();
								// take the first one
								if (pathTag != null) {
									postTagName = StringUtils.isBlank(postTagName) ? entry.getKey() : postTagName;

									tagMap.put(postTagName, pathTag);
									relateTraceId(responseHeader, postTagName, traceIdEnabled, pathTag);
									break;
								}
							}
						}
					}
				}
				break;
			default:
				break;
			}
		}

		TsfContext.putTags(tagMap, Tag.ControlFlag.TRANSITIVE);

		if (!responseHeader.isEmpty()) {
			Map<String, String> originalResponseHeaders = payload.getResponseHeaders();
			if (originalResponseHeaders == null) {
				payload.setResponseHeaders(responseHeader);
			}
			else {
				originalResponseHeaders.putAll(responseHeader);
			}
		}
		return payload;
	}

	private static void relateTraceId(Map<String, String> responseHeader, String postTagName, String traceIdEnabled,
			String tagValue) {
		if (PluginConstants.TraceIdEnabledType.Y.equals(PluginConstants.TraceIdEnabledType
				.getTraceIdEnabledType(traceIdEnabled))) {
			String traceId = Span.current().getSpanContext().getTraceId();
			responseHeader.put(postTagName, tagValue);
			responseHeader.put("X-Tsf-TraceId", traceId);
			logger.info("TraceId is {} , which is related to tag {}:{}", traceId, postTagName,
					tagValue);
		}
	}

	/**
	 * 断言Tag插件参数列表.
	 * @param tagPluginInfoListJson Tag插件参数列表Json串
	 * @return 是否符合格式
	 */
	public static boolean predicateJsonFormat(String tagPluginInfoListJson) {
		if (StringUtils.length(tagPluginInfoListJson) == 0) {
			return false;
		}
		List<TagPluginInfo> tagPluginInfos = null;
		try {
			tagPluginInfos = JacksonUtils.deserialize(tagPluginInfoListJson,
					new TypeReference<List<TagPluginInfo>>() { });
		}
		catch (Throwable t) {
			logger.error("deserialize tagPluginInfoList : {} occur exception : ", tagPluginInfoListJson, t);
			return false;
		}
		tagPluginInfos.forEach(tagPluginInfo -> {
			Optional.ofNullable(tagPluginInfo.getTraceIdEnabled())
					.ifPresent(PluginConstants.TraceIdEnabledType::checkValidity);
		});

		return true;
	}

	public static PluginPayload doRequestTransformer(
			RequestTransformerPluginInfo requestTransformerPluginInfo,
			TsfGatewayRequest tsfGatewayRequest,
			PluginPayload payload) {

		if (requestTransformerPluginInfo == null ||
				org.apache.commons.collections.CollectionUtils.isEmpty(requestTransformerPluginInfo.getActions())) {
			return payload;
		}

		Map<String, String> requestHeader = new HashMap<>();
		Map<String, String> tagMap = new HashMap<>(requestTransformerPluginInfo.getActions().size());

		boolean reqMatch = true;
		if (!org.apache.commons.collections.CollectionUtils.isEmpty(requestTransformerPluginInfo.getFilters())) {
			for (TransformerTag transformerTag : requestTransformerPluginInfo.getFilters()) {
				// 多个条件时是与关系，有一个为 false 时直接退出
				if (!reqMatch) {
					break;
				}
				switch (transformerTag.getTagPosition()) {
				case QUERY:
					Map<String, String[]> parameterMap = tsfGatewayRequest.getParameterMap();
					String[] queryTags = parameterMap.get(transformerTag.getTagField());
					String queryValue = null;
					if (queryTags != null) {
						queryValue = Joiner.on(",").join(queryTags);
					}
					reqMatch &= matchTag(transformerTag, queryValue);
					break;
				case COOKIE:
					String cookieTag = tsfGatewayRequest.getCookie(transformerTag.getTagField());
					reqMatch &= matchTag(transformerTag, cookieTag);
					break;
				case HEADER:
					String headerTag = tsfGatewayRequest.getRequestHeader(transformerTag.getTagField());
					reqMatch &= matchTag(transformerTag, headerTag);
					break;
				case PATH:
					String path = tsfGatewayRequest.getUri().getPath();
					if (StringUtils.isNotBlank(path)) {
						// ensure path start with '/'
						if (path.charAt(0) != '/') {
							path = "/" + path;
						}

						String apiPath = StringUtils.substring(path, StringUtils.ordinalIndexOf(path, "/", 4));
						boolean matched = antPathMatcher.match(transformerTag.getTagField(), apiPath);
						if (!matched) {
							logger.debug("[doRequestTransformer] path pattern not match, field:{}, apiPath:{}", transformerTag.getTagField(), apiPath);
							reqMatch = false;
							break;
						}
						Map<String, String> pathTagMap = antPathMatcher.extractUriTemplateVariables(transformerTag.getTagField(), apiPath);
						if (MapUtils.isNotEmpty(pathTagMap)) {
							for (Map.Entry<String, String> entry : pathTagMap.entrySet()) {
								String pathTag = entry.getValue();
								// take the first one
								reqMatch &= matchTag(transformerTag, pathTag);
								break;
							}
						}
					}
					break;
				default:
					break;

				}
			}
		}

		if (reqMatch) {
			// 每次请求只会进入这里一次，不需要像 RequestRouteDestRandomValueUtil 那样弄个 thread local 变量
			// 规则权重总和可能小于 100，但按 100 来随机
			int random = (int) (Math.random() * 100);
			int current = 0;
			for (TransformerAction action : requestTransformerPluginInfo.getActions()) {
				current += action.getWeight();
				// match
				if (random < current) {
					switch (action.getTagPosition()) {
					case TSF_TAG:
						tagMap.put(action.getTagName(), action.getTagValue());
						break;
					case HEADER:
						requestHeader.put(action.getTagName(), action.getTagValue());
						break;
					default:
						break;
					}
					// 命中权重，退出
					break;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("[doRequestTransformer] put tags:{}, reqMatch:{}", tagMap, reqMatch);
		}
		TsfContext.putTags(tagMap, Tag.ControlFlag.TRANSITIVE);

		if (!requestHeader.isEmpty()) {
			// request header 是网关向后请求时携带的，response header 是后端业务返回经过网关时携带的
			Map<String, String> originalRequestHeaders = payload.getRequestHeaders();
			if (originalRequestHeaders == null) {
				payload.setRequestHeaders(requestHeader);
			}
			else {
				originalRequestHeaders.putAll(requestHeader);
			}
		}
		return payload;
	}

	public static boolean matchTag(TagCondition transformerTag, String targetTagValue) {
		ModelProto.MatchString.MatchStringType matchType = TagConditionUtil.parseMatchStringType(transformerTag);

		return RuleUtils.matchStringValue(matchType, targetTagValue, transformerTag.getTagValue());
	}

}
