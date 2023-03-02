/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.discovery.refresh;

import java.lang.reflect.Field;
import java.util.Collections;

import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.pojo.ServicesByProto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static com.tencent.polaris.test.common.Consts.HOST;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PolarisServiceStatusChangeListener}.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceStatusChangeListenerTest {

	private ApplicationEventPublisher publisher;


	@BeforeEach
	void setUp() {
		publisher = mock(ApplicationEventPublisher.class);
		doNothing().when(publisher).publishEvent(any(ApplicationEvent.class));
	}

	@Test
	public void testOnResourceUpdated() {
		PolarisServiceStatusChangeListener polarisServiceStatusChangeListener = new PolarisServiceStatusChangeListener();
		polarisServiceStatusChangeListener.setApplicationEventPublisher(publisher);

		// Service update event
		ServiceEventKey serviceUpdateEventKey = new ServiceEventKey(
				new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER), ServiceEventKey.EventType.SERVICE);
		ServiceInfo serviceInfo = new ServiceInfo();
		serviceInfo.setNamespace(NAMESPACE_TEST);
		serviceInfo.setService(SERVICE_PROVIDER);
		// Need update
		ServicesByProto oldServices = new ServicesByProto(Collections.emptyList());
		ServicesByProto newServices = new ServicesByProto(Collections.singletonList(serviceInfo));
		polarisServiceStatusChangeListener.onResourceUpdated(serviceUpdateEventKey, oldServices, newServices);
		verify(publisher, times(1)).publishEvent(any(ApplicationEvent.class));
		// No need update
		oldServices = new ServicesByProto(Collections.singletonList(serviceInfo));
		newServices = new ServicesByProto(Collections.singletonList(serviceInfo));
		polarisServiceStatusChangeListener.onResourceUpdated(serviceUpdateEventKey, oldServices, newServices);
		verify(publisher, times(1)).publishEvent(any(ApplicationEvent.class));


		// Instance update event
		ServiceEventKey instanceUpdateEventKey = new ServiceEventKey(
				new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER), ServiceEventKey.EventType.INSTANCE);
		DefaultInstance instance = new DefaultInstance();
		instance.setNamespace(NAMESPACE_TEST);
		instance.setService(SERVICE_PROVIDER);
		instance.setHost(HOST);
		instance.setPort(PORT);
		try {
			Field instances = ServiceInstancesByProto.class.getDeclaredField("instances");
			instances.setAccessible(true);

			// Need update
			ServiceInstancesByProto oldInstances = new ServiceInstancesByProto();
			instances.set(oldInstances, Collections.emptyList());
			ServiceInstancesByProto newInstances = new ServiceInstancesByProto();
			instances.set(newInstances, Collections.singletonList(instance));
			polarisServiceStatusChangeListener.onResourceUpdated(instanceUpdateEventKey, oldInstances, newInstances);
			verify(publisher, times(2)).publishEvent(any(ApplicationEvent.class));

			// No need update
			oldInstances = new ServiceInstancesByProto();
			instances.set(oldInstances, Collections.singletonList(instance));
			newInstances = new ServiceInstancesByProto();
			instances.set(newInstances, Collections.singletonList(instance));
			polarisServiceStatusChangeListener.onResourceUpdated(instanceUpdateEventKey, oldInstances, newInstances);
			verify(publisher, times(2)).publishEvent(any(ApplicationEvent.class));
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			Assertions.fail("Exception encountered.", e);
		}
	}
}
