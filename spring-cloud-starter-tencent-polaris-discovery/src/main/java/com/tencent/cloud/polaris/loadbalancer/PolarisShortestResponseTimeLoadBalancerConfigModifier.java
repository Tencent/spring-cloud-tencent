package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.loadbalancer.shortestresponsetime.ShortestResponseTimeLoadBalanceConfig;


public class PolarisShortestResponseTimeLoadBalancerConfigModifier implements PolarisConfigModifier {

	private final PolairsShortestResponseTimeLoadBalancerProperties polairsShortestResponseTimeLoadBalancerProperties;

	public PolarisShortestResponseTimeLoadBalancerConfigModifier(
			PolairsShortestResponseTimeLoadBalancerProperties polairsShortestResponseTimeLoadBalancerProperties) {
		this.polairsShortestResponseTimeLoadBalancerProperties = polairsShortestResponseTimeLoadBalancerProperties;
	}
	@Override
	public void modify(ConfigurationImpl configuration) {
		ShortestResponseTimeLoadBalanceConfig config = new ShortestResponseTimeLoadBalanceConfig();
		config.setSlidePeriod(polairsShortestResponseTimeLoadBalancerProperties.getSlidePeriod());
		configuration.getConsumer().getLoadbalancer().setPluginConfig("shortestResponseTime",
				config);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
