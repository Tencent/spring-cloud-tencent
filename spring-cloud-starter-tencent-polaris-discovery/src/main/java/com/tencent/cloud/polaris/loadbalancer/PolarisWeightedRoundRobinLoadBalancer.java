package com.tencent.cloud.polaris.loadbalancer;

import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

/**
 * WeightedRoundRobin Loadbalancer of Polaris.
 *
 * @author <a href="mailto:veteranchen@tencent.com">veteranchen</a>
 */
public class PolarisWeightedRoundRobinLoadBalancer extends PolarisAbstractLoadBalancer {

	public PolarisWeightedRoundRobinLoadBalancer(String serviceId, ObjectProvider<ServiceInstanceListSupplier> supplierObjectProvider, RouterAPI routerAPI) {
		super(serviceId, supplierObjectProvider, routerAPI);

	}

	@Override
	protected ProcessLoadBalanceRequest setProcessLoadBalanceRequest(ProcessLoadBalanceRequest req) {
		req.setLbPolicy(LoadBalanceConfig.LOAD_BALANCE_WEIGHTED_ROUND_ROBIN);
		req.setCriteria(new Criteria());
		return req;
	}

}
