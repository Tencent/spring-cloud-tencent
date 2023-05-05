package com.tencent.cloud.tsf.adapter.config;

import com.tencent.polaris.assembly.flow.AssemblyFlow;
import com.tencent.polaris.client.api.SDKContext;

public class TsfAssemblyFlow implements AssemblyFlow {
	@Override
	public String getName() {
		return PolarisTsfFlowConfigModifier.TSF_FLOW_NAME;
	}

	@Override
	public void setSDKContext(SDKContext sdkContext) {

	}
}
