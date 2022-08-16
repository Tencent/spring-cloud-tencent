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

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.client.pojo.ServicesByProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

/**
 * Change listener of Polaris service info. When service info is created or deleted, or, instance of service is from
 * 0 to many.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceStatusChangeListener extends AbstractResourceEventListener implements ApplicationEventPublisherAware {

	/**
	 * Index of service info status.
	 */
	public static final AtomicLong INDEX = new AtomicLong(0);

	private static final Logger LOG = LoggerFactory.getLogger(PolarisServiceStatusChangeListener.class);

	private ApplicationEventPublisher publisher;

	@Override
	public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
			RegistryCacheValue newValue) {
		if (newValue.getEventType() == ServiceEventKey.EventType.SERVICE) {
			if (oldValue instanceof ServicesByProto && newValue instanceof ServicesByProto) {
				LOG.debug("receive service={} change event", svcEventKey);
				Set<String> oldServiceInfoSet = ((ServicesByProto) oldValue).getServices().stream()
						.map(i -> i.getNamespace() + "::" + i.getService()).collect(Collectors.toSet());
				Set<String> newServiceInfoSet = ((ServicesByProto) newValue).getServices().stream()
						.map(i -> i.getNamespace() + "::" + i.getService()).collect(Collectors.toSet());

				Sets.SetView<String> addServiceInfoSetView = Sets.difference(newServiceInfoSet, oldServiceInfoSet);
				Sets.SetView<String> deleteServiceInfoSetView = Sets.difference(oldServiceInfoSet, newServiceInfoSet);

				if (addServiceInfoSetView.isEmpty() && deleteServiceInfoSetView.isEmpty()) {
					return;
				}
				LOG.info("Service status is update. Add service of {}. Delete service of {}", addServiceInfoSetView, deleteServiceInfoSetView);

				// Trigger reload of gateway route cache.
				this.publisher.publishEvent(new HeartbeatEvent(this, INDEX.getAndIncrement()));
			}
		}
		else if (newValue.getEventType() == ServiceEventKey.EventType.INSTANCE) {
			if (oldValue instanceof ServiceInstancesByProto && newValue instanceof ServiceInstancesByProto) {
				LOG.debug("receive service instances={} change event", svcEventKey);
				ServiceInstancesByProto oldIns = (ServiceInstancesByProto) oldValue;
				ServiceInstancesByProto newIns = (ServiceInstancesByProto) newValue;
				if ((CollectionUtils.isEmpty(oldIns.getInstances())
						&& !CollectionUtils.isEmpty(newIns.getInstances())) ||
						(!CollectionUtils.isEmpty(oldIns.getInstances())
								&& CollectionUtils.isEmpty(newIns.getInstances()))) {
					LOG.info("Service status of {} is update.", newIns.getService());

					// Trigger reload of gateway route cache.
					this.publisher.publishEvent(new HeartbeatEvent(this, INDEX.getAndIncrement()));
				}
			}
		}
	}

	@Override
	public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
