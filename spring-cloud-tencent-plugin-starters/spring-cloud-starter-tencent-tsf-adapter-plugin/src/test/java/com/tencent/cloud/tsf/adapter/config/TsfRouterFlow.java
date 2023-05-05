package com.tencent.cloud.tsf.adapter.config;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.router.api.flow.RouterFlow;

public class TsfRouterFlow implements RouterFlow {

	@Override
	public String getName() {
		return PolarisTsfFlowConfigModifier.TSF_FLOW_NAME;
	}

	@Override
	public void setSDKContext(SDKContext sdkContext) {

	}
}
