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

package com.tencent.cloud.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.connector.consul.service.common.TagConstant;
import com.tencent.polaris.plugins.router.lane.LaneRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.tsf.core.entity.Metadata;
import org.springframework.tsf.core.entity.Tag;

public final class TsfTagUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TsfTagUtils.class);

	private TsfTagUtils() {
	}

	public static List<Tag> deserializeTagList(String buffer) {
		if (StringUtils.isEmpty(buffer)) {
			return null;
		}
		return Arrays.asList(JacksonUtils.deserialize(UrlUtils.decode(buffer), Tag[].class));
	}

	public static Metadata deserializeMetadata(String buffer) {
		if (StringUtils.isEmpty(buffer)) {
			return null;
		}
		return JacksonUtils.deserialize(UrlUtils.decode(buffer), Metadata.class);
	}

	public static Map<String, String> getTsfMetadataMap(Map<String, String> calleeTransitiveHeaders, Map<String, String> disposableMetadata,
			Map<String, String> customMetadata, Map<String, String> applicationMetadata) {

		Map<String, String> tsfMetadataMap = new HashMap<>();
		Set<String> tsfUserTagKeys = new HashSet<>();
		// user tags
		List<Tag> tsfUserTags = new ArrayList<>();
		List<Tag> tsfSystemTags = new ArrayList<>();
		Tag laneTag = null;

		for (Map.Entry<String, String> entry : customMetadata.entrySet()) {
			if (tsfUserTagKeys.contains(entry.getKey())) {
				continue;
			}
			Tag tag = new Tag(entry.getKey(),  entry.getValue(), Tag.ControlFlag.TRANSITIVE);
			tsfUserTags.add(tag);
			tsfUserTagKeys.add(entry.getKey());
			if (LaneRouter.TRAFFIC_STAIN_LABEL.equals(entry.getKey()) && entry.getValue().contains("/")) {
				String lane = entry.getValue().split("/")[1];
				laneTag = new Tag("lane", lane, Tag.ControlFlag.TRANSITIVE);
			}
		}
		for (Map.Entry<String, String> entry : disposableMetadata.entrySet()) {
			if (tsfUserTagKeys.contains(entry.getKey())) {
				continue;
			}
			Tag tag = new Tag(entry.getKey(),  entry.getValue());
			tsfUserTags.add(tag);
			tsfUserTagKeys.add(entry.getKey());
		}

		for (Map.Entry<String, String> entry : calleeTransitiveHeaders.entrySet()) {
			if (entry.getKey().startsWith(MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX)) {
				String key = entry.getKey().substring(MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX_LENGTH);
				String value = entry.getValue();
				if (tsfUserTagKeys.contains(key)) {
					continue;
				}

				Tag tag = new Tag(key, value, Tag.ControlFlag.TRANSITIVE);
				tsfUserTags.add(tag);
				tsfUserTagKeys.add(key);
				if (LaneRouter.TRAFFIC_STAIN_LABEL.equals(key) && value.contains("/")) {
					String lane = value.split("/")[1];
					laneTag = new Tag("lane", lane, Tag.ControlFlag.TRANSITIVE);

				}
			}
		}

		if (laneTag != null) {
			tsfSystemTags.add(laneTag);
		}

		if (CollectionUtils.isNotEmpty(tsfUserTags)) {
			tsfMetadataMap.put(MetadataConstant.HeaderName.TSF_TAGS, JacksonUtils.serialize2Json(tsfUserTags));
		}

		if (CollectionUtils.isNotEmpty(tsfSystemTags)) {
			tsfMetadataMap.put(MetadataConstant.HeaderName.TSF_SYSTEM_TAG, JacksonUtils.serialize2Json(tsfSystemTags));
		}

		Metadata metadata = new Metadata();
		for (Map.Entry<String, String> entry : applicationMetadata.entrySet()) {
			switch (entry.getKey()) {
			case MetadataConstants.LOCAL_SERVICE:
				metadata.setServiceName(entry.getValue());
				break;
			case MetadataConstants.LOCAL_IP:
				metadata.setLocalIp(entry.getValue());
				break;
			case TsfMetadataConstants.TSF_GROUP_ID:
				metadata.setGroupId(entry.getValue());
				break;
			case TsfMetadataConstants.TSF_APPLICATION_ID:
				metadata.setApplicationId(entry.getValue());
				break;
			case TsfMetadataConstants.TSF_INSTNACE_ID:
				metadata.setInstanceId(entry.getValue());
				break;
			case TsfMetadataConstants.TSF_PROG_VERSION:
				metadata.setApplicationVersion(entry.getValue());
				break;
			case TsfMetadataConstants.TSF_NAMESPACE_ID:
				metadata.setNamespaceId(entry.getValue());
				break;
			}
		}
		tsfMetadataMap.put(MetadataConstant.HeaderName.TSF_METADATA, JacksonUtils.serialize2Json(metadata));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("calleeTransitiveHeaders:{}, disposableMetadata: {}, customMetadata: {}, applicationMetadata: {}, tsfMetadataMap:{}",
					calleeTransitiveHeaders,  disposableMetadata, customMetadata, applicationMetadata, tsfMetadataMap);
		}

		return tsfMetadataMap;
	}

	public static void updateTsfMetadata(Map<String, String> mergedTransitiveMetadata,
			Map<String, String> mergedDisposableMetadata, Map<String, String> mergedApplicationMetadata, Map<String, String> addHeaders,
			AtomicReference<String> callerIp, String encodedUserTagList, String encodedSystemTagList, String encodedMetadata)  {

		if (!TsfContextUtils.isOnlyTsfConsulEnabled()) {
			return;
		}
		List<Tag> tsfUserTagList = TsfTagUtils.deserializeTagList(encodedUserTagList);
		int tagSize = Optional.ofNullable(tsfUserTagList).map(List::size).orElse(0);
		Map<String, String> tsfTransitiveMetadata = new HashMap<>(tagSize);
		Map<String, String> tsfDisposableMetadata = new HashMap<>(tagSize);
		if (CollectionUtils.isNotEmpty(tsfUserTagList)) {
			for (Tag tag : tsfUserTagList) {
				if (Tag.ControlFlag.TRANSITIVE.equals(tag.getFlags())) {
					tsfTransitiveMetadata.put(tag.getKey(), tag.getValue());
				}
				tsfDisposableMetadata.put(tag.getKey(), tag.getValue());
			}
			mergedTransitiveMetadata.putAll(tsfTransitiveMetadata);
			mergedDisposableMetadata.putAll(tsfDisposableMetadata);
		}

		List<Tag> tsfSystemTagList = TsfTagUtils.deserializeTagList(encodedSystemTagList);
		if (CollectionUtils.isNotEmpty(tsfSystemTagList)) {
			for (Tag tag : tsfSystemTagList) {
				if ("lane".equals(tag.getKey())) {
					mergedTransitiveMetadata.put(LaneRouter.TRAFFIC_STAIN_LABEL, UrlUtils.encode("tsf/" + tag.getValue()));
					addHeaders.put(MetadataConstant.POLARIS_TRANSITIVE_HEADER_PREFIX + LaneRouter.TRAFFIC_STAIN_LABEL,
							UrlUtils.encode("tsf/" + tag.getValue()));
				}
			}
		}

		Metadata metadata = TsfTagUtils.deserializeMetadata(encodedMetadata);
		if (metadata != null) {
			if (StringUtils.isNotEmpty(metadata.getLocalIp())) {
				callerIp.set(metadata.getLocalIp());
			}
			if (StringUtils.isNotEmpty(metadata.getApplicationId())) {
				mergedApplicationMetadata.put(TsfMetadataConstants.TSF_APPLICATION_ID, metadata.getApplicationId());
			}
			if (StringUtils.isNotEmpty(metadata.getGroupId())) {
				mergedApplicationMetadata.put(TsfMetadataConstants.TSF_GROUP_ID, metadata.getGroupId());
			}
			if (StringUtils.isNotEmpty(metadata.getApplicationVersion())) {
				mergedApplicationMetadata.put(TsfMetadataConstants.TSF_PROG_VERSION, metadata.getApplicationVersion());
			}
			if (StringUtils.isNotEmpty(metadata.getNamespaceId())) {
				mergedApplicationMetadata.put(TsfMetadataConstants.TSF_NAMESPACE_ID, metadata.getNamespaceId());
			}
			if (StringUtils.isNotEmpty(metadata.getServiceName())) {
				mergedApplicationMetadata.put(MetadataConstants.LOCAL_SERVICE, metadata.getServiceName());
				mergedApplicationMetadata.put(TagConstant.SYSTEM_FIELD.SOURCE_SERVICE_NAME, metadata.getServiceName());
			}
			if (StringUtils.isNotEmpty(metadata.getLocalIp())) {
				mergedApplicationMetadata.put(MetadataConstants.LOCAL_IP, metadata.getLocalIp());
			}
		}

		if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("mergedTransitiveMetadata:{}, mergedDisposableMetadata: {}, mergedApplicationMetadata: {},"
								+ " addHeaders:{}, encodedUserTagList:{}, encodedSystemTagList:{}, encodedMetadata:{}, callerIp:{}",
						mergedTransitiveMetadata, mergedDisposableMetadata, mergedApplicationMetadata,
						addHeaders, encodedUserTagList, encodedSystemTagList, encodedMetadata, callerIp.get());
		}

	}

}
