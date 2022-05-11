package com.tencent.cloud.polaris.registry;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.client.pojo.ServicesByProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * Change listener of Polaris service info.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceChangeListener extends AbstractResourceEventListener implements ApplicationEventPublisherAware {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisServiceChangeListener.class);

	private static final AtomicInteger INDEX = new AtomicInteger(0);

	private ApplicationEventPublisher publisher;

	@Override
	public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
			RegistryCacheValue newValue) {
		if (newValue.getEventType() != ServiceEventKey.EventType.SERVICE) {
			return;
		}
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
			LOG.info("Service info is update. Add service of {}. Delete service of {}", addServiceInfoSetView, deleteServiceInfoSetView);

			// Trigger reload of gateway route cache.
			this.publisher.publishEvent(new HeartbeatEvent(this, INDEX.getAndIncrement()));
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
