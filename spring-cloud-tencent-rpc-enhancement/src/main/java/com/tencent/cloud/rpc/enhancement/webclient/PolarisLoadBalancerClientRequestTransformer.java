package com.tencent.cloud.rpc.enhancement.webclient;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.web.reactive.function.client.ClientRequest;

public class PolarisLoadBalancerClientRequestTransformer implements LoadBalancerClientRequestTransformer {

	@Override
	public ClientRequest transformRequest(ClientRequest request, ServiceInstance instance) {
		if (instance != null) {
			MetadataContextHolder.get().setLoadbalancer(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID, instance.getServiceId());
		}
		return request;
	}

}
