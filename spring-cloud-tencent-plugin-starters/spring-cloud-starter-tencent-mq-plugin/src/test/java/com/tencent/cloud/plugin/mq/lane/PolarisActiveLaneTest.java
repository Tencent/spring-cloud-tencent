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
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.mq.lane.kafka.KafkaLaneProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.cloud.client.serviceregistry.Registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisActiveLane}.
 * Instance and service status change.
 */
public class PolarisActiveLaneTest {

	private static final String CURRENT_INSTANCE_ID = "current-instance-id";

	private KafkaLaneProperties kafkaLaneProperties;
	private PolarisActiveLane polarisActiveLane;
	private PolarisSDKContextManager polarisSDKContextManager;
	private Registration registration;
	private LaneProto.LaneGroup group;
	private MockedStatic<LaneUtils> laneUtilsMockedStatic;
	private MockedStatic<JacksonUtils> jacksonUtilsMockedStatic;
	private MockedStatic<TsfContextUtils> tsfContextUtilsMockedStatic;

	@BeforeEach
	public void setUp() throws Exception {
		group = mock(LaneProto.LaneGroup.class);
		kafkaLaneProperties = new KafkaLaneProperties();
		kafkaLaneProperties.setLaneOn(true);
		polarisSDKContextManager = mock(PolarisSDKContextManager.class);
		when(polarisSDKContextManager.getSDKContext()).thenReturn(mock(SDKContext.class));
		registration = mock(Registration.class);
		when(registration.getInstanceId()).thenReturn(CURRENT_INSTANCE_ID);
		polarisActiveLane = new PolarisActiveLane(polarisSDKContextManager, mock(PolarisDiscoveryHandler.class), kafkaLaneProperties, registration);
		laneUtilsMockedStatic = Mockito.mockStatic(LaneUtils.class);
		jacksonUtilsMockedStatic = Mockito.mockStatic(JacksonUtils.class);
		tsfContextUtilsMockedStatic = Mockito.mockStatic(TsfContextUtils.class);

		jacksonUtilsMockedStatic.when(() -> JacksonUtils.serialize2Json(any())).thenReturn("{}");

	}

	@AfterEach
	public void tearDown() {
		laneUtilsMockedStatic.close();
		jacksonUtilsMockedStatic.close();
		tsfContextUtilsMockedStatic.close();
	}

	@Test
	public void testCallback() throws Throwable {
		// not in lane
		assertThat(polarisActiveLane.currentInstanceInLane()).isFalse();

		// in lane
		Map<String, String> metadata = new HashMap<>();
		metadata.put("lane", "test-lane");

		Instance instance = mock(Instance.class);
		when(instance.getId()).thenReturn(CURRENT_INSTANCE_ID);
		when(instance.getMetadata()).thenReturn(metadata);
		laneUtilsMockedStatic.when(() -> LaneUtils.getLaneGroups(any(), any()))
				.thenReturn(Collections.singletonList(group));

		polarisActiveLane.callback(Collections.singletonList(instance));

		assertThat(polarisActiveLane.currentInstanceInLane()).isTrue();

		// instance not in lane
		metadata.remove("lane");
		polarisActiveLane.callback(Collections.singletonList(instance));
		assertThat(polarisActiveLane.currentInstanceInLane()).isFalse();

		// reset, instance in lane
		metadata.put("lane", "test-lane");
		polarisActiveLane.callback(Collections.singletonList(instance));
		assertThat(polarisActiveLane.currentInstanceInLane()).isTrue();

		// service not in lane
		laneUtilsMockedStatic.when(() -> LaneUtils.getLaneGroups(any(), any())).thenReturn(Collections.emptyList());
		polarisActiveLane.freshLaneStatus(); // rule listener will call this method
		assertThat(polarisActiveLane.currentInstanceInLane()).isFalse();
	}
}
