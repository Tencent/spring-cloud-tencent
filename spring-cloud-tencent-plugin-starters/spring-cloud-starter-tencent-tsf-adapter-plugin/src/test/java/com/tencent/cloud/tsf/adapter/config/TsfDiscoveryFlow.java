package com.tencent.cloud.tsf.adapter.config;

import com.tencent.polaris.api.flow.DiscoveryFlow;
import com.tencent.polaris.client.api.SDKContext;

public class TsfDiscoveryFlow implements DiscoveryFlow {


	@Override
	public String getName() {
		return PolarisTsfFlowConfigModifier.TSF_FLOW_NAME;
	}

	@Override
	public void setSDKContext(SDKContext sdkContext) {

	}
}
