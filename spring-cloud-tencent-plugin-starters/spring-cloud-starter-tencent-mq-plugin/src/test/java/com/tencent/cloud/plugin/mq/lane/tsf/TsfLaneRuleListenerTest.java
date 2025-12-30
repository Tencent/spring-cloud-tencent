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

import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link TsfLaneRuleListener}.
 */
public class TsfLaneRuleListenerTest {

	private TsfActiveLane tsfActiveLane;
	private TsfLaneRuleListener tsfLaneRuleListener;

	@BeforeEach
	public void setUp() {
		tsfActiveLane = mock(TsfActiveLane.class);
		tsfLaneRuleListener = new TsfLaneRuleListener(tsfActiveLane);
	}

	@Test
	public void testConstructorInitialization() {
		// Verify that constructor properly initializes the TsfActiveLane dependency
		assert tsfLaneRuleListener != null;
	}

	@Test
	public void testOnResourceAddWithLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue newValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.LANE_RULE);

		// When
		tsfLaneRuleListener.onResourceAdd(svcEventKey, newValue);

		// Then
		verify(tsfActiveLane).freshLaneStatus();
	}

	@Test
	public void testOnResourceAddWithNonLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue newValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.INSTANCE);

		// When
		tsfLaneRuleListener.onResourceAdd(svcEventKey, newValue);

		// Then
		verify(tsfActiveLane, never()).freshLaneStatus();
	}

	@Test
	public void testOnResourceUpdatedWithLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue oldValue = mock(RegistryCacheValue.class);
		RegistryCacheValue newValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.LANE_RULE);

		// When
		tsfLaneRuleListener.onResourceUpdated(svcEventKey, oldValue, newValue);

		// Then
		verify(tsfActiveLane).freshLaneStatus();
	}

	@Test
	public void testOnResourceUpdatedWithNonLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue oldValue = mock(RegistryCacheValue.class);
		RegistryCacheValue newValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.SERVICE);

		// When
		tsfLaneRuleListener.onResourceUpdated(svcEventKey, oldValue, newValue);

		// Then
		verify(tsfActiveLane, never()).freshLaneStatus();
	}

	@Test
	public void testOnResourceDeletedWithLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue oldValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.LANE_RULE);

		// When
		tsfLaneRuleListener.onResourceDeleted(svcEventKey, oldValue);

		// Then
		verify(tsfActiveLane).freshLaneStatus();
	}

	@Test
	public void testOnResourceDeletedWithNonLaneRuleEvent() {
		// Given
		ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
		RegistryCacheValue oldValue = mock(RegistryCacheValue.class);

		when(svcEventKey.getEventType()).thenReturn(ServiceEventKey.EventType.ROUTING);

		// When
		tsfLaneRuleListener.onResourceDeleted(svcEventKey, oldValue);

		// Then
		verify(tsfActiveLane, never()).freshLaneStatus();
	}

	@Test
	public void testEventTypeCoverage() {
		// Test all event types to ensure proper filtering
		for (ServiceEventKey.EventType eventType : ServiceEventKey.EventType.values()) {
			ServiceEventKey svcEventKey = mock(ServiceEventKey.class);
			RegistryCacheValue value = mock(RegistryCacheValue.class);

			when(svcEventKey.getEventType()).thenReturn(eventType);

			// When
			tsfLaneRuleListener.onResourceAdd(svcEventKey, value);

			// Then
			if (eventType == ServiceEventKey.EventType.LANE_RULE) {
				verify(tsfActiveLane).freshLaneStatus();
			}
			else {
				verify(tsfActiveLane, never()).freshLaneStatus();
			}

			// Reset mock for next iteration
			Mockito.reset(tsfActiveLane);
		}
	}
}
