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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.plugin.mq.lane.tsf.TsfActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
 * Test for {@link KafkaLaneAspect}.
 */
public class KafkaLaneAspectTest {

	private KafkaLaneAspect kafkaLaneAspect;
	private PolarisSDKContextManager polarisSDKContextManager;
	private KafkaLaneProperties kafkaLaneProperties;
	private TsfActiveLane tsfActiveLane;
	private MockedStatic<LaneUtils> laneUtilsMockedStatic;
	private MockedStatic<TsfContextUtils> tsfContextUtilsMockedStatic;

	@BeforeEach
	public void setUp() {
		polarisSDKContextManager = mock(PolarisSDKContextManager.class);
		kafkaLaneProperties = new KafkaLaneProperties();
		tsfActiveLane = mock(TsfActiveLane.class);

		laneUtilsMockedStatic = Mockito.mockStatic(LaneUtils.class);
		tsfContextUtilsMockedStatic = Mockito.mockStatic(TsfContextUtils.class);

		kafkaLaneAspect = new KafkaLaneAspect(polarisSDKContextManager, kafkaLaneProperties, tsfActiveLane);
	}

	@AfterEach
	public void tearDown() {
		laneUtilsMockedStatic.close();
		tsfContextUtilsMockedStatic.close();
		resetTsfContextUtilsStaticFields();
	}

	private void resetTsfContextUtilsStaticFields() {
		try {
			// Reset isOnlyTsfConsulEnabledFirstConfiguration
			Field isOnlyTsfConsulEnabledFirstConfigurationField = TsfContextUtils.class.getDeclaredField("isOnlyTsfConsulEnabledFirstConfiguration");
			isOnlyTsfConsulEnabledFirstConfigurationField.setAccessible(true);
			AtomicBoolean isOnlyTsfConsulEnabledFirstConfiguration = (AtomicBoolean) isOnlyTsfConsulEnabledFirstConfigurationField.get(null);
			isOnlyTsfConsulEnabledFirstConfiguration.set(true);

			// Reset isTsfConsulEnabledFirstConfiguration
			Field isTsfConsulEnabledFirstConfigurationField = TsfContextUtils.class.getDeclaredField("isTsfConsulEnabledFirstConfiguration");
			isTsfConsulEnabledFirstConfigurationField.setAccessible(true);
			AtomicBoolean isTsfConsulEnabledFirstConfiguration = (AtomicBoolean) isTsfConsulEnabledFirstConfigurationField.get(null);
			isTsfConsulEnabledFirstConfiguration.set(true);

			// Reset tsfConsulEnabled
			Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
			tsfConsulEnabledField.setAccessible(true);
			tsfConsulEnabledField.setBoolean(null, false);

			// Reset onlyTsfConsulEnabled
			Field onlyTsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("onlyTsfConsulEnabled");
			onlyTsfConsulEnabledField.setAccessible(true);
			onlyTsfConsulEnabledField.setBoolean(null, false);

		}
		catch (Exception e) {
			throw new RuntimeException("Failed to reset TsfContextUtils static fields", e);
		}
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
		Iterator<Header> headers = producerRecord.headers().headers("X-Polaris-Metadata-Transitive-service-lane").iterator();
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
	public void testConsumerAspectWithLaneHeader() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);
		String laneId = "test-lane-id";

		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		consumerRecord.headers().add("X-Polaris-Metadata-Transitive-service-lane", laneId.getBytes(StandardCharsets.UTF_8));

		ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
		Object[] args = new Object[] {consumerRecord};
		when(pjp.getArgs()).thenReturn(args);
		when(pjp.proceed(args)).thenReturn("result");

		// When
		Object result = kafkaLaneAspect.aroundConsumerMessage(pjp);

		// Then
		assertThat(result).isEqualTo(KafkaLaneAspect.EMPTY_OBJECT);
	}

	@Test
	public void testGetConsumerRecordLaneIdFromHeader() {
		// Given
		ConsumerRecord consumerRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
		String expectedLaneId = "test-lane-id";
		consumerRecord.headers().add("X-Polaris-Metadata-Transitive-service-lane", expectedLaneId.getBytes(StandardCharsets.UTF_8));

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
		assertThat(laneId).isEqualTo("tsf/" + expectedLaneId);
	}

	@Test
	public void testIfConsumeInPolarisWithNoLaneId() {
		// Given
		kafkaLaneProperties.setLaneConsumeMain(true);

		// Use reflection to set the lane field
		try {
			Field laneField = KafkaLaneAspect.class.getDeclaredField("lane");
			laneField.setAccessible(true);
			laneField.set(kafkaLaneAspect, "test-lane");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		// When
		boolean shouldConsume = kafkaLaneAspect.ifConsume("");

		// Then
		assertThat(shouldConsume).isTrue(); // Because laneConsumeMain is true
	}

	@Test
	public void testConsumerAspectWithBatchMessages() throws Throwable {
		// Given
		kafkaLaneProperties.setLaneOn(true);

		List<ConsumerRecord> messageList = new ArrayList<>();
		ConsumerRecord record1 = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value1");
		record1.headers().add("X-Polaris-Metadata-Transitive-service-lane", "lane1".getBytes(StandardCharsets.UTF_8));
		ConsumerRecord record2 = new ConsumerRecord<>("test-topic", 0, 1L, "key", "value2");
		record2.headers().add("X-Polaris-Metadata-Transitive-service-lane", "lane2".getBytes(StandardCharsets.UTF_8));

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
