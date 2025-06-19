package com.tencent.cloud.polaris.loadbalancer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.polaris.loadbalancer")
public class PolairsShortestResponseTimeLoadBalancerProperties {
	/**
	 * Slide period in milliseconds.
	 */
	@Value("${spring.cloud.polaris.loadbalancer.polarisShortestResponseTime.slidePeriod:30000}")
	private long slidePeriod;

	long getSlidePeriod() {
		return slidePeriod;
	}

	void setSlidePeriod(long slidePeriod) {
		this.slidePeriod = slidePeriod;
	}


}
