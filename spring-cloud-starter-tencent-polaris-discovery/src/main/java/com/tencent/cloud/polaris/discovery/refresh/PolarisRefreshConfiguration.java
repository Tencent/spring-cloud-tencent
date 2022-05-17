package com.tencent.cloud.polaris.discovery.refresh;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for listening the change of service status.
 *
 * @author Haotian Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
public class PolarisRefreshConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PolarisServiceStatusChangeListener polarisServiceChangeListener() {
		return new PolarisServiceStatusChangeListener();
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisRefreshApplicationReadyEventListener polarisServiceStatusApplicationReadyEventListener(
			PolarisDiscoveryHandler polarisDiscoveryHandler,
			PolarisServiceStatusChangeListener polarisServiceStatusChangeListener) {
		return new PolarisRefreshApplicationReadyEventListener(polarisDiscoveryHandler, polarisServiceStatusChangeListener);
	}
}
