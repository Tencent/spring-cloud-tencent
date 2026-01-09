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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.plugin.mq.lane.AbstractActiveLane;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.plugins.router.lane.LaneUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

@Aspect
@Order(1)
public class KafkaLaneAspect {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaLaneAspect.class);
	/**
	 * Empty object.
	 */
	public static final Object EMPTY_OBJECT = new Object();

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final KafkaLaneProperties kafkaLaneProperties;

	private final AbstractActiveLane activeLane;

	private final String laneHeaderKey;

	public KafkaLaneAspect(PolarisSDKContextManager polarisSDKContextManager, KafkaLaneProperties kafkaLaneProperties, AbstractActiveLane activeLane) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.kafkaLaneProperties = kafkaLaneProperties;
		this.activeLane = activeLane;
		laneHeaderKey = activeLane.getLaneHeaderKey();
	}

	@Pointcut("execution(* org.springframework.kafka.core.KafkaTemplate.send(..))")
	private void producerPointcut() {
	}

	@Around("producerPointcut()")
	public Object aroundProducerMessage(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		KafkaTemplate target = (KafkaTemplate) pjp.getTarget();

		if (!this.kafkaLaneProperties.getLaneOn()) {
			return pjp.proceed(args);
		}

		String laneId = LaneUtils.fetchLaneByCaller(
				Optional.ofNullable(polarisSDKContextManager).map(PolarisSDKContextManager::getSDKContext).map(SDKContext::getExtensions).orElse(null),
				com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE,
				com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE);

		if (StringUtils.isBlank(laneId) || !kafkaLaneProperties.getAutoSetHeader()) {
			return pjp.proceed(args);
		}

		LOG.debug("kafka producer lane before, args: {}, thread laneId: {}", args, laneId);

		try {
			ProducerRecord producerRecord;
			if (args.length == 1) {
				// ListenableFuture<SendResult<K, V>> send(ProducerRecord<K, V> record); ListenableFuture<SendResult<K, V>> send(Message<?> message);
				if (args[0] instanceof Message) {
					Message message = (Message) args[0];
					producerRecord = target.getMessageConverter().fromMessage(message, target.getDefaultTopic());
					// possibly no Jackson
					if (!producerRecord.headers().iterator().hasNext()) {
						byte[] correlationId = message.getHeaders().get(KafkaHeaders.CORRELATION_ID, byte[].class);
						if (correlationId != null) {
							producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId);
						}
					}
				}
				else {
					producerRecord = (ProducerRecord) args[0];
				}
			}
			else if (args.length == 2) {
				// ListenableFuture<SendResult<K, V>> send(String topic, V data);
				producerRecord = new ProducerRecord<>((String) args[0], args[1]);
			}
			else if (args.length == 3) {
				// ListenableFuture<SendResult<K, V>> send(String topic, K key, V data);
				producerRecord = new ProducerRecord<>((String) args[0], args[1], args[2]);
			}
			else if (args.length == 4) {
				// ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, K key, V data);
				producerRecord = new ProducerRecord<>((String) args[0], (Integer) args[1], args[2], args[3]);
			}
			else if (args.length == 5) {
				// ListenableFuture<SendResult<K, V>> send(String topic, Integer partition, Long timestamp, K key, V data);
				producerRecord = new ProducerRecord<>((String) args[0], (Integer) args[1], (Long) args[2], args[3], args[4]);
			}
			else {
				LOG.error("KafkaTemplate send message with wrong args: {}", args);
				return pjp.proceed(args);
			}

			Header laneHeader = new RecordHeader(laneHeaderKey, laneId.getBytes(StandardCharsets.UTF_8));
			producerRecord.headers().add(laneHeader);

			LOG.debug("kafka producer lane after, args: {}, laneId: {}", producerRecord, laneId);

			return target.send(producerRecord);
		}
		catch (Exception e) {
			LOG.error("add laneId to kafka message error", e);
		}

		return pjp.proceed(args);
	}

	@Pointcut("@annotation(org.springframework.kafka.annotation.KafkaListener)")
	private void consumerPointcut() {
	}

	@Around("consumerPointcut()")
	public Object aroundConsumerMessage(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();

		if (!this.kafkaLaneProperties.getLaneOn()) {
			return pjp.proceed(args);
		}
		// init metadata context
		MetadataContextHolder.get();

		try {
			ConsumerRecord consumerRecord = null;
			List messageList = null;
			int dataPosition = -1;
			Acknowledgment acknowledgment = null;
			for (int i = 0; i < args.length; i++) {
				if ((args[i] instanceof Acknowledgment)) {
					acknowledgment = (Acknowledgment) args[i];
				}
				else if ((args[i] instanceof ConsumerRecord)) {
					consumerRecord = (ConsumerRecord) args[i];
					dataPosition = i;
				}
				else if (args[i] instanceof List) {
					messageList = (List) args[i];
					dataPosition = i;
				}
			}

			// parameter is message list, for batch consume
			if (messageList != null) {
				// empty list directly return
				if (messageList.isEmpty()) {
					return pjp.proceed(args);
				}

				// parameter is not consumerRecord, consume directly
				if (!(messageList.get(0) instanceof ConsumerRecord)) {
					return pjp.proceed(args);
				}

				List<ConsumerRecord> newMessageList = new ArrayList<>();
				for (Object item : messageList) {
					ConsumerRecord record = (ConsumerRecord) item;
					String laneId = this.getConsumerRecordLaneId(record);

					boolean ifConsume = this.ifConsume(laneId);
					if (ifConsume) {
						newMessageList.add(record);
					}
					else {
						if (acknowledgment != null) {
							acknowledgment.acknowledge();
						}
						if (LOG.isDebugEnabled()) {
							LOG.debug("not need consume, laneId: {}, message:{}", laneId, record);
						}
					}
				}
				args[dataPosition] = newMessageList;
			}

			// parameter is consumerRecord
			if (consumerRecord != null) {
				String laneId = this.getConsumerRecordLaneId(consumerRecord);
				boolean ifConsume = this.ifConsume(laneId);
				if (!ifConsume) {
					if (acknowledgment != null) {
						acknowledgment.acknowledge();
					}
					if (LOG.isDebugEnabled()) {
						LOG.debug("not need consume, laneId: {}, message:{}", laneId, consumerRecord);
					}
					return EMPTY_OBJECT;
				}
			}
		}
		catch (Exception e) {
			LOG.error("extract laneId from kafka message error", e);
		}

		Object result = pjp.proceed(args);
		LaneUtils.removeCallerLaneId();
		return result;
	}

	String getConsumerRecordLaneId(ConsumerRecord consumerRecord) {

		String laneId = null;

		Iterator<Header> iterator = consumerRecord.headers().headers(laneHeaderKey).iterator();
		if (iterator.hasNext()) {
			laneId = new String(iterator.next().value(), StandardCharsets.UTF_8);
		}
		// falls back to the laneId from the metadata context (set by the user's aspect) if the header is empty
		if (StringUtils.isBlank(laneId)) {
			laneId = LaneUtils.getCallerLaneId();
		}
		// format laneId
		laneId = activeLane.formatLaneId(laneId);
		return laneId;
	}

	/**
	 * whether the message is consumed by the current listener.
	 * @param messageLaneId  message lane id.
	 * @return whether to consume.
	 */
	boolean ifConsume(String messageLaneId) {
		return activeLane.ifConsume(messageLaneId);
	}
}
