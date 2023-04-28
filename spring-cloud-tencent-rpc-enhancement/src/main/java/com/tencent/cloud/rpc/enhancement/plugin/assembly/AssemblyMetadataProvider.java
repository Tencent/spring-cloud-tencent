package com.tencent.cloud.rpc.enhancement.plugin.assembly;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.MetadataProvider;

import org.springframework.cloud.client.ServiceInstance;

public class AssemblyMetadataProvider implements MetadataProvider {

	private final ServiceInstance serviceInstance;

	private final String namespace;

	public AssemblyMetadataProvider(ServiceInstance localServiceInstance, String namespace) {
		this.serviceInstance = localServiceInstance;
		this.namespace = namespace;
	}

	@Override
	public String getMetadata(String key) {
		return serviceInstance.getMetadata().get(key);
	}

	@Override
	public ServiceKey getLocalService() {
		return new ServiceKey(namespace, serviceInstance.getServiceId());
	}

	@Override
	public String getLocalIp() {
		return serviceInstance.getHost();
	}

}
