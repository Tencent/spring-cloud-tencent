package com.tencent.cloud.polaris.discovery.refresh;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import static com.tencent.cloud.polaris.discovery.refresh.PolarisServiceStatusChangeListener.INDEX;

/**
 * Begin refresh when application is ready.
 *
 * @author Haotian Zhang
 */
public class PolarisRefreshApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent>, ApplicationEventPublisherAware {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisRefreshConfiguration.class);
	private static final int DELAY = 60;
	private final PolarisDiscoveryHandler polarisDiscoveryHandler;
	private final PolarisServiceStatusChangeListener polarisServiceStatusChangeListener;
	private final ScheduledExecutorService refreshExecutor;
	private ApplicationEventPublisher publisher;

	public PolarisRefreshApplicationReadyEventListener(PolarisDiscoveryHandler polarisDiscoveryHandler, PolarisServiceStatusChangeListener polarisServiceStatusChangeListener) {
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
		this.polarisServiceStatusChangeListener = polarisServiceStatusChangeListener;
		this.refreshExecutor = Executors.newSingleThreadScheduledExecutor(
				new NamedThreadFactory("polaris-service-refresh"));
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Register service change listener.
		polarisDiscoveryHandler.getSdkContext().getExtensions().getLocalRegistry()
				.registerResourceListener(polarisServiceStatusChangeListener);

		// Begin scheduled refresh thread.
		refresh();
	}

	/**
	 * Start the refresh thread.
	 */
	public void refresh() {
		refreshExecutor.scheduleWithFixedDelay(() -> {
			try {
				// Trigger reload of gateway route cache.
				this.publisher.publishEvent(new HeartbeatEvent(this, INDEX.getAndIncrement()));
			}
			catch (Exception e) {
				LOG.error("refresh polaris service error.", e);
			}
		}, DELAY, DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
