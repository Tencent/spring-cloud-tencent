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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.plugin.mq.lane.PolarisActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link KafkaLaneAspect}. Instance in lane.
 */
public class KafkaLaneAspectTest {

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
		polarisActiveLane = new PolarisActiveLane(mock(PolarisSDKContextManager.class), mock(PolarisDiscoveryHandler.class), mock(Registration.class));
		laneUtilsMockedStatic = Mockito.mockStatic(LaneUtils.class);
		tsfContextUtilsMockedStatic = Mockito.mockStatic(TsfContextUtils.class);
		Field laneField = PolarisActiveLane.class.getDeclaredField("instanceLaneTag");
		laneField.setAccessible(true);
		laneField.set(polarisActiveLane, "test-lane"); // in lane

		Field groupsField = PolarisActiveLane.class.getDeclaredField("groups");
		groupsField.setAccessible(true);
		groupsField.set(polarisActiveLane, Collections.singletonList(group));

		Field serviceInLaneField = PolarisActiveLane.class.getDeclaredField("serviceInLane");
		serviceInLaneField.setAccessible(true);
		serviceInLaneField.setBoolean(polarisActiveLane, true);


		kafkaLaneAspect = new KafkaLaneAspect(polarisSDKContextManager, kafkaLaneProperties, polarisActiveLane);
	}

	@AfterEach
	public void tearDown() {
		laneUtilsMockedStatic.close();
		tsfContextUtilsMockedStatic.close();
	}

	@Test
	public void testProducerAspectWhenLaneDisabled() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(false);
		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {mock(ProducerRecord.class)};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundProducerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
		verify(pjp).proceed(args);
	}

	@Test
	public void testProducerAspectWhenNoLaneId() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);
		laneUtilsMockedStatic.when(() -> LaneUtils.fetchLaneByCaller(any(), any(), any())).thenReturn(null);

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {mock(ProducerRecord.class)};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundProducerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
		verify(pjp).proceed(args);
	}

	@Test
	public void testProducerAspectWithProducerRecord() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);
		String laneId = "test-lane-id";
		laneUtilsMockedStatic.when(() -> LaneUtils.fetchLaneByCaller(any(), any(), any())).thenReturn(laneId);

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
		ProducerRecord producerRecord = new ProducerRecord<>("test-topic", "test-value");

		when(pjp.getTarget()).thenReturn(kafkaTemplate);
		when(pjp.getArgs()).thenReturn(new Object[] {producerRecord});
		when(kafkaTemplate.send(producerRecord)).thenReturn(null);

		// When
		kafkaLaneAspect.aroundProducerMessage(pjp);

		// Then
		Iterator<Header> headers = producerRecord.headers().headers(polarisActiveLane.getLaneHeaderKey()).iterator();
		assertThat(headers.hasNext()).isTrue();
		Header laneHeader = headers.next();
		assertThat(new String(laneHeader.value(), StandardCharsets.UTF_8)).isEqualTo(laneId);
	}

	@Test
	public void testProducerAspectWithMessage() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);
		String laneId = "test-lane-id";
		laneUtilsMockedStatic.when(() -> LaneUtils.fetchLaneByCaller(any(), any(), any())).thenReturn(laneId);

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		KafkaTemplate kafkaTemplate = mock(KafkaTemplate.class);
		RecordMessageConverter recordMessageConverter = mock(RecordMessageConverter.class);
		Message message = MessageBuilder.withPayload("test-payload").build();
		ProducerRecord producerRecord = new ProducerRecord<>("test-topic", "test-value");

		when(pjp.getTarget()).thenReturn(kafkaTemplate);
		when(pjp.getArgs()).thenReturn(new Object[] {message});
		when(kafkaTemplate.getDefaultTopic()).thenReturn("test-topic");
		when(kafkaTemplate.send(producerRecord)).thenReturn(null);
		when(kafkaTemplate.getMessageConverter()).thenReturn(recordMessageConverter);

		// When
		kafkaLaneAspect.aroundProducerMessage(pjp);
	}

	@Test
	public void testConsumerAspectWhenLaneDisabled() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(false);
		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {mock(ConsumerRecord.class)};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
		verify(pjp).proceed(args);
	}

	@Test
	public void testConsumerAspectWithLaneHeader_LaneIdExist() throws Throwable {
		// Given
		LaneProto.LaneRule rule = mock(LaneProto.LaneRule.class);
		when(group.getRulesList()).thenReturn(Collections.singletonList(rule));
		when(rule.getGroupName()).thenReturn("test-group");
		when(rule.getName()).thenReturn("test-lane-name");
		when(rule.getDefaultLabelValue()).thenReturn("test-lane");
		kafkaLaneProperties.setLaneOn(true);
		String laneId = "test-group/test-lane-name"; // valid lane id

		laneUtilsMockedStatic.when(() -> LaneUtils.buildStainLabel(rule)).thenReturn(laneId);


		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		consumerRecord.headers().add(polarisActiveLane.getLaneHeaderKey(), laneId.getBytes(StandardCharsets.UTF_8));

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {consumerRecord};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
	}

	@Test
	public void testGetConsumerRecordLaneIdFromHeader() {
		// Given
		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		String expectedLaneId = "test-lane-id";
		consumerRecord.headers()
				.add(polarisActiveLane.getLaneHeaderKey(), expectedLaneId.getBytes(StandardCharsets.UTF_8));

		// When
		String laneId = kafkaLaneAspect.getConsumerRecordLaneId(consumerRecord);

		// Then
		assertThat(laneId).isEqualTo(expectedLaneId);
	}

	@Test
	public void testGetConsumerRecordLaneIdFromCallerLane() {
		// Given
		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		String expectedLaneId = "lane-test";
		laneUtilsMockedStatic.when(() -> LaneUtils.getCallerLaneId()).thenReturn(expectedLaneId);

		// When
		String laneId = kafkaLaneAspect.getConsumerRecordLaneId(consumerRecord);

		// Then
		assertThat(laneId).isEqualTo(expectedLaneId);
	}

	@Test
	public void testIfConsumeWithNoLaneId() {
		// Given
		kafkaLaneProperties.setLaneConsumeMain(true);
		// When
		boolean shouldConsume = kafkaLaneAspect.ifConsume("");
		// Then
		assertThat(shouldConsume).isTrue(); // Because laneConsumeMain is true

		// Given
		kafkaLaneProperties.setLaneConsumeMain(false);
		// When
		shouldConsume = kafkaLaneAspect.ifConsume("");
		// Then
		assertThat(shouldConsume).isFalse(); // Because laneConsumeMain is false
	}

	@Test
	public void testConsumerAspectWithBatchMessages() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);

		List<ConsumerRecord> messageList = new ArrayList<>();
		ConsumerRecord record1 = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value1");
		record1.headers().add(polarisActiveLane.getLaneHeaderKey(), "lane1".getBytes(StandardCharsets.UTF_8));
		ConsumerRecord record2 = new ConsumerRecord<>("test-topic", 0, 1L, "key", "value2");
		record2.headers().add(polarisActiveLane.getLaneHeaderKey(), "lane2".getBytes(StandardCharsets.UTF_8));

		messageList.add(record1);
		messageList.add(record2);

		Acknowledgment acknowledgment = mock(Acknowledgment.class);
		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {messageList, acknowledgment};

		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(any())).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
		verify(pjp).proceed(any());
	}

	@Test
	public void testConsumerAspectWithEmptyBatch() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);

		List<ConsumerRecord> emptyList = new ArrayList<>();
		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {emptyList};

		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// Then
		assertThat(result).isEqualTo("result");
		verify(pjp).proceed(args);
	}
}
