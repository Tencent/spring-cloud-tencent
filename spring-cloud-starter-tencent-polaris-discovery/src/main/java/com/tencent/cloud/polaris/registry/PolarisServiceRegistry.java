/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.registry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.polaris.util.OkHttpUtil;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;

/**
 * Service registry of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisServiceRegistry implements ServiceRegistry<Registration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisServiceRegistry.class);

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	private final StaticMetadataManager staticMetadataManager;

	private final ScheduledExecutorService heartbeatExecutor;

	public PolarisServiceRegistry(PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisDiscoveryHandler polarisDiscoveryHandler,
			StaticMetadataManager staticMetadataManager) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
		this.staticMetadataManager = staticMetadataManager;

		if (polarisDiscoveryProperties.isHeartbeatEnabled()) {
			this.heartbeatExecutor = Executors
					.newSingleThreadScheduledExecutor(new NamedThreadFactory("polaris-heartbeat"));
		}
		else {
			this.heartbeatExecutor = null;
		}
	}

	@Override
	public void register(Registration registration) {
		if (StringUtils.isEmpty(registration.getServiceId())) {
			LOGGER.warn("No service to register for polaris client...");
			return;
		}
		// Register instance.
		InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
		instanceRegisterRequest.setNamespace(polarisDiscoveryProperties.getNamespace());
		instanceRegisterRequest.setService(registration.getServiceId());
		instanceRegisterRequest.setHost(registration.getHost());
		instanceRegisterRequest.setPort(registration.getPort());
		instanceRegisterRequest.setWeight(polarisDiscoveryProperties.getWeight());
		instanceRegisterRequest.setToken(polarisDiscoveryProperties.getToken());
		instanceRegisterRequest.setRegion(staticMetadataManager.getRegion());
		instanceRegisterRequest.setZone(staticMetadataManager.getZone());
		instanceRegisterRequest.setCampus(staticMetadataManager.getCampus());
		if (null != heartbeatExecutor) {
			instanceRegisterRequest.setTtl(polarisDiscoveryProperties.getHeartbeatInterval());
		}
		instanceRegisterRequest.setMetadata(registration.getMetadata());
		instanceRegisterRequest.setProtocol(polarisDiscoveryProperties.getProtocol());
		instanceRegisterRequest.setVersion(polarisDiscoveryProperties.getVersion());
		try {
			ProviderAPI providerClient = polarisDiscoveryHandler.getProviderAPI();
			providerClient.register(instanceRegisterRequest);
			LOGGER.info("polaris registry, {} {} {}:{} {} register finished",
					polarisDiscoveryProperties.getNamespace(),
					registration.getServiceId(), registration.getHost(),
					registration.getPort(), staticMetadataManager.getMergedStaticMetadata());

			if (null != heartbeatExecutor) {
				InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
				BeanUtils.copyProperties(instanceRegisterRequest, heartbeatRequest);
				// Start the heartbeat thread after the registration is successful.
				heartbeat(heartbeatRequest);
			}
		}
		catch (Exception e) {
			LOGGER.error("polaris registry, {} register failed...{},", registration.getServiceId(), registration, e);
			rethrowRuntimeException(e);
		}
	}

	@Override
	public void deregister(Registration registration) {
		LOGGER.info("De-registering from Polaris Server now...");

		if (StringUtils.isEmpty(registration.getServiceId())) {
			LOGGER.warn("No dom to de-register for polaris client...");
			return;
		}

		InstanceDeregisterRequest deRegisterRequest = new InstanceDeregisterRequest();
		deRegisterRequest.setToken(polarisDiscoveryProperties.getToken());
		deRegisterRequest.setNamespace(polarisDiscoveryProperties.getNamespace());
		deRegisterRequest.setService(registration.getServiceId());
		deRegisterRequest.setHost(registration.getHost());
		deRegisterRequest.setPort(registration.getPort());

		try {
			ProviderAPI providerClient = polarisDiscoveryHandler.getProviderAPI();
			providerClient.deRegister(deRegisterRequest);
		}
		catch (Exception e) {
			LOGGER.error("ERR_POLARIS_DEREGISTER, de-register failed...{},", registration, e);
		}
		finally {
			if (null != heartbeatExecutor) {
				heartbeatExecutor.shutdown();
			}
		}
		LOGGER.info("De-registration finished.");
	}

	@Override
	public void close() {

	}

	@Override
	public void setStatus(Registration registration, String status) {

	}

	@Override
	public Object getStatus(Registration registration) {
		String serviceName = registration.getServiceId();
		InstancesResponse instancesResponse = polarisDiscoveryHandler.getInstances(serviceName);
		Instance[] instances = instancesResponse.getInstances();
		if (null == instances || instances.length == 0) {
			return null;
		}
		for (Instance instance : instances) {
			if (instance.getHost().equalsIgnoreCase(registration.getHost())
					&& instance.getPort() == polarisDiscoveryProperties.getPort()) {
				return instance.isHealthy() ? "UP" : "DOWN";
			}
		}
		return null;
	}

	/**
	 * Start the heartbeat thread.
	 * @param heartbeatRequest heartbeat request
	 */
	public void heartbeat(InstanceHeartbeatRequest heartbeatRequest) {
		heartbeatExecutor.scheduleWithFixedDelay(() -> {
			try {
				String healthCheckEndpoint = polarisDiscoveryProperties.getHealthCheckUrl();
				// First determine whether health-check-url is configured.
				// If configured, the service instance health check needs to be executed
				// first.
				// If the health check passes, the heartbeat will be reported.
				// If it does not pass, the heartbeat will not be reported.
				if (StringUtils.isNotBlank(healthCheckEndpoint)) {
					if (!healthCheckEndpoint.startsWith("/")) {
						healthCheckEndpoint = "/" + healthCheckEndpoint;
					}

					String healthCheckUrl = String.format("http://%s:%s%s",
							heartbeatRequest.getHost(), heartbeatRequest.getPort(), healthCheckEndpoint);

					if (!OkHttpUtil.get(healthCheckUrl, null)) {
						LOGGER.error("backend service health check failed. health check endpoint = {}", healthCheckEndpoint);
						return;
					}
				}

				polarisDiscoveryHandler.getProviderAPI().heartbeat(heartbeatRequest);
				LOGGER.trace("Polaris heartbeat is sent");
			}
			catch (PolarisException e) {
				LOGGER.error("polaris heartbeat error with code [{}]", e.getCode(), e);
			}
			catch (Exception e) {
				LOGGER.error("polaris heartbeat runtime error", e);
			}
		}, polarisDiscoveryProperties.getHeartbeatInterval(), polarisDiscoveryProperties.getHeartbeatInterval(), SECONDS);
	}
}
