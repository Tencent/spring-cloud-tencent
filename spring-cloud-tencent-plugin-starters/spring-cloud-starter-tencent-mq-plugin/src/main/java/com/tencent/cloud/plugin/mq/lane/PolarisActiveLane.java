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

package com.tencent.cloud.plugin.mq.lane;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.mq.lane.kafka.KafkaLaneProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.plugins.router.lane.LaneRouter;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.serviceregistry.Registration;


public class PolarisActiveLane extends AbstractActiveLane implements InitializingBean {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisActiveLane.class);

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final PolarisDiscoveryHandler discoveryClient;

	private final KafkaLaneProperties kafkaLaneProperties;

	@Value("${spring.application.name:}")
	private String springApplicationName;

	private volatile String lane = "";

	private volatile boolean serviceInLane = false;

	private volatile List<LaneProto.LaneGroup> groups;

	private Registration registration;

	public PolarisActiveLane(PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryHandler discoveryClient,
			KafkaLaneProperties kafkaLaneProperties, Registration registration) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.discoveryClient = discoveryClient;
		this.kafkaLaneProperties = kafkaLaneProperties;
		this.registration = registration;
		Optional.ofNullable(polarisSDKContextManager).map(PolarisSDKContextManager::getSDKContext)
				.map(SDKContext::getExtensions).map(Extensions::getLocalRegistry)
				.ifPresent(localRegistry -> localRegistry.registerResourceListener(new LaneRuleListener(this::freshLaneStatus)));

		Optional.ofNullable(polarisSDKContextManager).map(PolarisSDKContextManager::getSDKContext)
				.map(SDKContext::getExtensions).map(Extensions::getLocalRegistry)
				.ifPresent(localRegistry -> localRegistry.registerResourceListener(this));
	}

	@Override
	public void afterPropertiesSet() {
		// get instances to trigger callback when instances change
		discoveryClient.getHealthyInstances(springApplicationName);
	}

	@Override
	public void callback(List<Instance> currentServiceInstances) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("currentServices: {}", JacksonUtils.serialize2Json(currentServiceInstances));
		}

		freshWhenInstancesChange(currentServiceInstances);

		if (LOG.isDebugEnabled()) {
			LOG.debug("current lane:{},  serviceInLane: {}", lane, serviceInLane);
		}
	}

	private void freshWhenInstancesChange(List<Instance> currentServices) {
		if (currentServices == null || currentServices.isEmpty()) {
			return;
		}

		// get all active groups
		for (Instance healthService : currentServices) {
			if (StringUtils.equals(healthService.getId(), registration.getInstanceId())) {
				this.lane = healthService.getMetadata().get("lane");
				break;
			}
		}

		freshLaneStatus();
	}

	/**
	 * update lane status.
	 */
	public void freshLaneStatus() {

		ServiceKey localService = new ServiceKey(com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE,
				com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE);

		groups = LaneUtils.getLaneGroups(localService, polarisSDKContextManager.getSDKContext().getExtensions());

		serviceInLane = CollectionUtils.isNotEmpty(groups);
	}

	public boolean currentInstanceInLane() {
		return StringUtils.isNotEmpty(lane) && serviceInLane;
	}

	public String getLane() {
		return lane;
	}

	public List<LaneProto.LaneGroup> getGroups() {
		return groups == null ? Collections.emptyList() : groups;
	}

	@Override
	public boolean ifConsume(String messageLaneId) {
		// message has no lane id
		if (StringUtils.isEmpty(messageLaneId)) {
			if (!currentInstanceInLane()) {
				// baseline service, consume directly
				return true;
			}
			else {
				// lane listener consumes baseline message
				return this.kafkaLaneProperties.getLaneConsumeMain();
			}
		}
		else {
			LaneUtils.setCallerLaneId(messageLaneId);

			// message has lane id
			if (!currentInstanceInLane()) {
				// baseline service
				return this.kafkaLaneProperties.getMainConsumeLane();
			}
			else {
				// whether the message lane id is the same as the lane id of the listener
				for (LaneProto.LaneGroup group : getGroups()) {
					for (LaneProto.LaneRule rule : group.getRulesList()) {
						if (StringUtils.equals(messageLaneId, LaneUtils.buildStainLabel(rule))
								&& StringUtils.equals(rule.getDefaultLabelValue(), getLane())) {
							return true;
						}
					}
				}
				return false;
			}
		}
	}

	@Override
	public String getLaneHeaderKey() {
		return MetadataContext.DEFAULT_TRANSITIVE_PREFIX + LaneRouter.TRAFFIC_STAIN_LABEL;
	}
}
