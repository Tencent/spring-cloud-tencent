package com.tencent.cloud.polaris.registry.filter;

import com.tencent.polaris.api.rpc.InstanceRegisterRequest;

public class RegisterFilterHandlerContext {
	private InstanceRegisterRequest instanceRegisterRequest;

	public InstanceRegisterRequest getInstanceRegisterRequest() {
		return instanceRegisterRequest;
	}

	public RegisterFilterHandlerContext(InstanceRegisterRequest instanceRegisterRequest) {
		this.instanceRegisterRequest = instanceRegisterRequest;
	}
}
