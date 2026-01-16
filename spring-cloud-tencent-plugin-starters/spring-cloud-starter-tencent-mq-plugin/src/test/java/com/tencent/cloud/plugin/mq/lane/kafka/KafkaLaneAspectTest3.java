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

package com.tencent.cloud.plugin.mq.lane.kafka;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.plugin.mq.lane.PolarisActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.cloud.client.serviceregistry.Registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link KafkaLaneAspect}.
 * Instance and service status change.
 */
public class KafkaLaneAspectTest3 {

	private KafkaLaneAspect kafkaLaneAspect;
	private PolarisSDKContextManager polarisSDKContextManager;
	private KafkaLaneProperties kafkaLaneProperties;
	private PolarisActiveLane polarisActiveLane;
	private LaneProto.LaneGroup group;
	private MockedStatic<LaneUtils> laneUtilsMockedStatic;
	private MockedStatic<TsfContextUtils> tsfContextUtilsMockedStatic;

	@BeforeEach
	public void setUp() throws Exception {
		polarisSDKContextManager = mock(PolarisSDKContextManager.class);
		group = mock(LaneProto.LaneGroup.class);
		kafkaLaneProperties = new KafkaLaneProperties();
		kafkaLaneProperties.setLaneOn(true);
		polarisActiveLane = new PolarisActiveLane(mock(PolarisSDKContextManager.class), mock(PolarisDiscoveryHandler.class), kafkaLaneProperties, mock(Registration.class));
		laneUtilsMockedStatic = Mockito.mockStatic(LaneUtils.class);
		tsfContextUtilsMockedStatic = Mockito.mockStatic(TsfContextUtils.class);


		kafkaLaneAspect = new KafkaLaneAspect(polarisSDKContextManager, kafkaLaneProperties, polarisActiveLane);
	}

	@AfterEach
	public void tearDown() {
		laneUtilsMockedStatic.close();
		tsfContextUtilsMockedStatic.close();
	}

	@Test
	public void testConsumerAspectWithLaneHeader_toInLane() throws Throwable {
		// not in lane
		Field laneField = PolarisActiveLane.class.getDeclaredField("instanceLaneTag");
		laneField.setAccessible(true);
		laneField.set(polarisActiveLane, "");

		Field groupsField = PolarisActiveLane.class.getDeclaredField("groups");
		groupsField.setAccessible(true);
		groupsField.set(polarisActiveLane, Collections.singletonList(group));

		Field serviceInLaneField = PolarisActiveLane.class.getDeclaredField("serviceInLane");
		serviceInLaneField.setAccessible(true);
		serviceInLaneField.setBoolean(polarisActiveLane, false);

		String laneId = "test-group/test-lane-name"; // valid lane id
		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		consumerRecord.headers().add(polarisActiveLane.getLaneHeaderKey(), laneId.getBytes(StandardCharsets.UTF_8));

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {consumerRecord};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// act
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// verify
		assertThat(result).isEqualTo(KafkaLaneAspect.EMPTY_OBJECT); // not in lane, not consume

		// in lane

		LaneProto.LaneRule rule = mock(LaneProto.LaneRule.class);
		when(group.getRulesList()).thenReturn(Collections.singletonList(rule));
		when(rule.getGroupName()).thenReturn("test-group");
		when(rule.getName()).thenReturn("test-lane-name");
		when(rule.getDefaultLabelValue()).thenReturn("test-lane");

		laneField.set(polarisActiveLane, "test-lane");
		serviceInLaneField.setBoolean(polarisActiveLane, true);

		laneUtilsMockedStatic.when(() -> LaneUtils.buildStainLabel(rule)).thenReturn(laneId);

		result = kafkaLaneAspect.aroundConsumerMessage(pjp);
		assertThat(result).isEqualTo("result");
	}

	@Test
	public void testConsumerAspectWithLaneHeader_toNotInLane() throws Throwable {
		// in lane
		String laneId = "test-group/test-lane-name"; // valid lane id
		LaneProto.LaneRule rule = mock(LaneProto.LaneRule.class);
		when(group.getRulesList()).thenReturn(Collections.singletonList(rule));
		when(rule.getGroupName()).thenReturn("test-group");
		when(rule.getName()).thenReturn("test-lane-name");
		when(rule.getDefaultLabelValue()).thenReturn("test-lane");
		laneUtilsMockedStatic.when(() -> LaneUtils.buildStainLabel(rule)).thenReturn(laneId);

		Field laneField = PolarisActiveLane.class.getDeclaredField("instanceLaneTag");
		laneField.setAccessible(true);
		laneField.set(polarisActiveLane, "test-lane");

		Field groupsField = PolarisActiveLane.class.getDeclaredField("groups");
		groupsField.setAccessible(true);
		groupsField.set(polarisActiveLane, Collections.singletonList(group));

		Field serviceInLaneField = PolarisActiveLane.class.getDeclaredField("serviceInLane");
		serviceInLaneField.setAccessible(true);
		serviceInLaneField.setBoolean(polarisActiveLane, true);


		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		consumerRecord.headers().add(polarisActiveLane.getLaneHeaderKey(), laneId.getBytes(StandardCharsets.UTF_8));

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {consumerRecord};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// act
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// verify
		assertThat(result).isEqualTo("result");


		// not in lane
		when(group.getRulesList()).thenReturn(Collections.emptyList());
		laneField.set(polarisActiveLane, "");
		serviceInLaneField.setBoolean(polarisActiveLane, false);

		result = kafkaLaneAspect.aroundConsumerMessage(pjp);
		assertThat(result).isEqualTo(KafkaLaneAspect.EMPTY_OBJECT);
	}
}
