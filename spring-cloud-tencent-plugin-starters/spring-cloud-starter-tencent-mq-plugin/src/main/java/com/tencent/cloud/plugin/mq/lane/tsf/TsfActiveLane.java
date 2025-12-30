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

package com.tencent.cloud.plugin.mq.lane.tsf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.discovery.refresh.ServiceInstanceChangeCallback;
import com.tencent.cloud.polaris.discovery.refresh.ServiceInstanceChangeListener;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;


@ServiceInstanceChangeListener(serviceName = "${spring.application.name}")
public class TsfActiveLane implements ServiceInstanceChangeCallback, InitializingBean {

	private static final Logger LOG = LoggerFactory.getLogger(TsfActiveLane.class);

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final PolarisDiscoveryHandler discoveryClient;
	/**
	 * Online deployment groups for this service (same namespace id and application id required).
	 */
	private volatile Set<String> activeGroupSet = new HashSet<>();

	private volatile Set<String> currentGroupLaneIds = null;
	/**
	 * key: laneId.
	 * value: true - online, false - offline.
	 */
	private volatile Map<String, Boolean> laneActiveMap = new HashMap<>();

	@Value("${tsf_namespace_id:}")
	private String tsfNamespaceId;

	@Value("${tsf_group_id:}")
	private String tsfGroupId;

	@Value("${tsf_application_id:}")
	private String tsfApplicationId;

	@Value("${spring.application.name:}")
	private String springApplicationName;

	public TsfActiveLane(PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryHandler discoveryClient) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.discoveryClient = discoveryClient;
		Optional.ofNullable(polarisSDKContextManager).map(PolarisSDKContextManager::getSDKContext)
				.map(SDKContext::getExtensions).map(Extensions::getLocalRegistry)
				.ifPresent(localRegistry -> localRegistry.registerResourceListener(new TsfLaneRuleListener(this)));
	}

	@Override
	public void afterPropertiesSet() {
		// get instances to trigger callback when instances change
		discoveryClient.getHealthyInstances(springApplicationName);
	}

	@Override
	public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("ConsulServiceChangeCallback currentServices: {}", JacksonUtils.serialize2Json(currentServiceInstances));
			LOG.debug("current namespaceId: {}, groupId: {}, applicationId: {}", tsfNamespaceId, tsfGroupId, tsfApplicationId);
		}

		freshWhenInstancesChange(currentServiceInstances);

		if (LOG.isDebugEnabled()) {
			LOG.info("current lane active status: {}", JacksonUtils.serialize2Json(laneActiveMap));
		}
	}

	private void freshWhenInstancesChange(List<Instance> currentServices) {
		if (currentServices == null || currentServices.isEmpty()) {
			return;
		}

		Set<String> currentActiveGroupSet = new HashSet<>();

		// get all active groups
		for (Instance healthService : currentServices) {
			String nsId = healthService.getMetadata().get("TSF_NAMESPACE_ID");
			String groupId = healthService.getMetadata().get("TSF_GROUP_ID");
			String applicationId = healthService.getMetadata().get("TSF_APPLICATION_ID");
			if (tsfNamespaceId.equals(nsId) && tsfApplicationId.equals(applicationId) && StringUtils.isNotEmpty(groupId)) {
				currentActiveGroupSet.add(groupId);
			}
		}

		activeGroupSet = currentActiveGroupSet;

		freshLaneStatus();
	}

	/**
	 * update lane status.
	 */
	public void freshLaneStatus() {
		Map<String, Boolean> currentLaneActiveMap = new HashMap<>();
		Set<String> tempCurrentGroupLaneIds = new HashSet<>();

		ServiceKey localService = new ServiceKey(com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE,
				com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE);

		List<LaneProto.LaneGroup> groups = LaneUtils.getLaneGroups(localService, polarisSDKContextManager.getSDKContext().getExtensions());


		for (LaneProto.LaneGroup laneGroup : groups) {
			for (LaneProto.LaneRule laneRule : laneGroup.getRulesList()) {
				// in tsf, if namespace id and application id are in lane rule, it means the service is in lane.
				if (!laneGroup.getMetadataMap().containsKey(tsfNamespaceId + "," + tsfApplicationId)) {
					continue;
				}
				// in tsf, lane label key is TsfMetadataConstants.TSF_GROUP_ID
				if (!TsfMetadataConstants.TSF_GROUP_ID.equals(laneRule.getLabelKey())
						|| StringUtils.isEmpty(laneRule.getDefaultLabelValue())) {
					continue;
				}

				for (String groupId : laneRule.getDefaultLabelValue().split(",")) {
					if (activeGroupSet.contains(groupId)) {
						// active group, update lane active status
						currentLaneActiveMap.put(laneRule.getId(), true);
					}
					else {
						// inactive group, mark lane as inactive only if no other active group exists
						currentLaneActiveMap.putIfAbsent(laneRule.getId(), false);
					}
					if (StringUtils.equals(groupId, tsfGroupId)) {
						tempCurrentGroupLaneIds.add(laneRule.getId());
					}
				}
			}
		}

		laneActiveMap = currentLaneActiveMap;
		currentGroupLaneIds = tempCurrentGroupLaneIds;
	}

	public boolean isLaneExist(String laneId) {
		return laneActiveMap.containsKey(laneId);
	}

	public boolean isActiveLane(String laneId) {
		return laneActiveMap.getOrDefault(laneId, false);
	}

	public Set<String> getCurrentGroupLaneIds() {
		return currentGroupLaneIds;
	}

}
