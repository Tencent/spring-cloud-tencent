package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

/**
 * Polaris Least Connection LoadBalancer.
 *
 * @author Yuwei Fu
 */
public class PolarisLeastConnectionLoadBalancer extends AbstractPolarisLoadBalancer {
	public PolarisLeastConnectionLoadBalancer(String serviceId, ObjectProvider<ServiceInstanceListSupplier> supplierObjectProvider, RouterAPI routerAPI) {
		super(serviceId, supplierObjectProvider, routerAPI);
	}

	@Override
	protected ProcessLoadBalanceRequest setProcessLoadBalanceRequest(ProcessLoadBalanceRequest req) {
		req.setLbPolicy(LoadBalanceConfig.LOAD_BALANCE_LEAST_CONNECTION);
		req.setCriteria(new Criteria());
		return req;
	}
}
