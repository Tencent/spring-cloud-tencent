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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.metadata.core.constant.TsfMetadataConstants;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link TsfActiveLane}.
 */
public class TsfActiveLaneTest {

	private TsfActiveLane tsfActiveLane;
	private PolarisSDKContextManager polarisSDKContextManager;
	private PolarisDiscoveryHandler discoveryClient;
	private SDKContext sdkContext;
	private Extensions extensions;
	private MockedStatic<LaneUtils> laneUtilsMockedStatic;

	@BeforeEach
	public void setUp() {
		polarisSDKContextManager = mock(PolarisSDKContextManager.class);
		discoveryClient = mock(PolarisDiscoveryHandler.class);
		sdkContext = mock(SDKContext.class);
		extensions = mock(Extensions.class);

		laneUtilsMockedStatic = Mockito.mockStatic(LaneUtils.class);

		when(polarisSDKContextManager.getSDKContext()).thenReturn(sdkContext);
		when(sdkContext.getExtensions()).thenReturn(extensions);

		tsfActiveLane = new TsfActiveLane(polarisSDKContextManager, discoveryClient);

		// Set up field values using reflection
		ReflectionTestUtils.setField(tsfActiveLane, "tsfNamespaceId", "test-namespace");
		ReflectionTestUtils.setField(tsfActiveLane, "tsfGroupId", "test-group");
		ReflectionTestUtils.setField(tsfActiveLane, "tsfApplicationId", "test-app");
		ReflectionTestUtils.setField(tsfActiveLane, "springApplicationName", "test-service");
	}

	@AfterEach
	public void tearDown() {
		laneUtilsMockedStatic.close();
	}

	@Test
	public void testConstructorInitialization() {
		// Verify that constructor properly initializes dependencies
		assertThat(tsfActiveLane).isNotNull();
		verify(polarisSDKContextManager, times(1)).getSDKContext();
		verify(sdkContext, times(1)).getExtensions();
	}

	@Test
	public void testCallbackWithEmptyInstances() {
		// Given
		List<Instance> currentInstances = Collections.emptyList();
		List<Instance> addInstances = Collections.emptyList();
		List<Instance> deleteInstances = Collections.emptyList();

		// When
		tsfActiveLane.callback(currentInstances, addInstances, deleteInstances);

		// Then
		// Should not throw any exceptions and handle empty instances gracefully
	}

	@Test
	public void testFreshLaneStatusWithActiveGroups() {
		// Given
		Set<String> activeGroups = new HashSet<>(Arrays.asList("group1", "group2"));
		ReflectionTestUtils.setField(tsfActiveLane, "activeGroupSet", activeGroups);

		// Mock lane groups and rules
		LaneProto.LaneGroup laneGroup = mock(LaneProto.LaneGroup.class);
		LaneProto.LaneRule laneRule = mock(LaneProto.LaneRule.class);

		when(laneGroup.getMetadataMap()).thenReturn(createMetadataMap("test-namespace,test-app"));
		when(laneGroup.getRulesList()).thenReturn(Collections.singletonList(laneRule));
		when(laneRule.getLabelKey()).thenReturn(TsfMetadataConstants.TSF_GROUP_ID);
		when(laneRule.getDefaultLabelValue()).thenReturn("test-group,group2,group3");
		when(laneRule.getId()).thenReturn("lane1");

		laneUtilsMockedStatic.when(() -> LaneUtils.getLaneGroups(any(ServiceKey.class), any(Extensions.class)))
				.thenReturn(Collections.singletonList(laneGroup));

		// When
		ReflectionTestUtils.invokeMethod(tsfActiveLane, "freshLaneStatus");

		// Then
		Map<String, Boolean> laneActiveMap = (Map<String, Boolean>) ReflectionTestUtils.getField(tsfActiveLane, "laneActiveMap");
		Set<String> currentGroupLaneIds = (Set<String>) ReflectionTestUtils.getField(tsfActiveLane, "currentGroupLaneIds");

		assertThat(laneActiveMap).containsEntry("lane1", true);
		assertThat(currentGroupLaneIds).contains("lane1");
	}

	@Test
	public void testFreshLaneStatusWithInactiveGroups() {
		// Given
		Set<String> activeGroups = new HashSet<>(Collections.singletonList("group1"));
		ReflectionTestUtils.setField(tsfActiveLane, "activeGroupSet", activeGroups);

		// Mock lane groups and rules
		LaneProto.LaneGroup laneGroup = mock(LaneProto.LaneGroup.class);
		LaneProto.LaneRule laneRule = mock(LaneProto.LaneRule.class);

		when(laneGroup.getMetadataMap()).thenReturn(createMetadataMap("test-namespace,test-app"));
		when(laneGroup.getRulesList()).thenReturn(Collections.singletonList(laneRule));
		when(laneRule.getLabelKey()).thenReturn(TsfMetadataConstants.TSF_GROUP_ID);
		when(laneRule.getDefaultLabelValue()).thenReturn("group2,group3");
		when(laneRule.getId()).thenReturn("lane1");

		laneUtilsMockedStatic.when(() -> LaneUtils.getLaneGroups(any(ServiceKey.class), any(Extensions.class)))
				.thenReturn(Collections.singletonList(laneGroup));

		// When
		ReflectionTestUtils.invokeMethod(tsfActiveLane, "freshLaneStatus");

		// Then
		Map<String, Boolean> laneActiveMap = (Map<String, Boolean>) ReflectionTestUtils.getField(tsfActiveLane, "laneActiveMap");
		assertThat(laneActiveMap).containsEntry("lane1", false);
	}

	@Test
	public void testIsLaneExist() {
		// Given
		Map<String, Boolean> laneActiveMap = new HashMap<>();
		laneActiveMap.put("lane1", true);
		laneActiveMap.put("lane2", false);
		ReflectionTestUtils.setField(tsfActiveLane, "laneActiveMap", laneActiveMap);

		// When & Then
		assertThat(tsfActiveLane.isLaneExist("lane1")).isTrue();
		assertThat(tsfActiveLane.isLaneExist("lane2")).isTrue();
		assertThat(tsfActiveLane.isLaneExist("lane3")).isFalse();
	}

	@Test
	public void testIsActiveLane() {
		// Given
		Map<String, Boolean> laneActiveMap = new HashMap<>();
		laneActiveMap.put("lane1", true);
		laneActiveMap.put("lane2", false);
		ReflectionTestUtils.setField(tsfActiveLane, "laneActiveMap", laneActiveMap);

		// When & Then
		assertThat(tsfActiveLane.isActiveLane("lane1")).isTrue();
		assertThat(tsfActiveLane.isActiveLane("lane2")).isFalse();
		assertThat(tsfActiveLane.isActiveLane("lane3")).isFalse();
	}

	@Test
	public void testGetCurrentGroupLaneIds() {
		// Given
		Set<String> currentGroupLaneIds = new TreeSet<>(Arrays.asList("lane1", "lane2"));
		ReflectionTestUtils.setField(tsfActiveLane, "currentGroupLaneIds", currentGroupLaneIds);

		// When
		Set<String> result = tsfActiveLane.getCurrentGroupLaneIds();

		// Then
		assertThat(result).containsExactly("lane1", "lane2");
	}

	@Test
	public void testFreshWhenInstancesChangeWithEmptyList() {
		// Given
		List<Instance> emptyInstances = Collections.emptyList();

		// When
		ReflectionTestUtils.invokeMethod(tsfActiveLane, "freshWhenInstancesChange", emptyInstances);

		// Then
		// Should handle empty list gracefully without exceptions
	}

	@Test
	public void testFreshWhenInstancesChangeWithValidInstances() {
		// Given
		Instance instance = mock(Instance.class);
		when(instance.getMetadata()).thenReturn(createMetadata("test-namespace", "group1", "test-app"));

		List<Instance> instances = Collections.singletonList(instance);

		// When
		ReflectionTestUtils.invokeMethod(tsfActiveLane, "freshWhenInstancesChange", instances);

		// Then
		Set<String> activeGroupSet = (Set<String>) ReflectionTestUtils.getField(tsfActiveLane, "activeGroupSet");
		assertThat(activeGroupSet).contains("group1");
	}

	private Map<String, String> createMetadata(String namespaceId, String groupId, String applicationId) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("TSF_NAMESPACE_ID", namespaceId);
		metadata.put("TSF_GROUP_ID", groupId);
		metadata.put("TSF_APPLICATION_ID", applicationId);
		return metadata;
	}

	private Map<String, String> createMetadataMap(String value) {
		Map<String, String> metadataMap = new HashMap<>();
		metadataMap.put("test-namespace,test-app", value);
		return metadataMap;
	}
}
