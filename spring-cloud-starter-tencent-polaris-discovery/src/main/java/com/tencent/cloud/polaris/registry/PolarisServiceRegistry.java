/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 */

package com.tencent.cloud.polaris.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.OkHttpUtil;
import com.tencent.cloud.common.util.OtUtils;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatProperties;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.factory.config.provider.ServiceConfigImpl;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service registry of Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng, changjin wei(魏昌进)
 */
public class PolarisServiceRegistry implements ServiceRegistry<PolarisRegistration>, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisServiceRegistry.class);

	private final PolarisDiscoveryProperties polarisDiscoveryProperties;

	private final PolarisSDKContextManager polarisSDKContextManager;

	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	private final StaticMetadataManager staticMetadataManager;
	private final PolarisStatProperties polarisStatProperties;

	public PolarisServiceRegistry(PolarisDiscoveryProperties polarisDiscoveryProperties,
			PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryHandler polarisDiscoveryHandler,
			StaticMetadataManager staticMetadataManager, PolarisStatProperties polarisStatProperties) {
		this.polarisDiscoveryProperties = polarisDiscoveryProperties;
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
		this.staticMetadataManager = staticMetadataManager;
		this.polarisStatProperties = polarisStatProperties;
	}

	@Override
	public void register(PolarisRegistration registration) {
		if (StringUtils.isBlank(registration.getServiceId())) {
			LOGGER.warn("No service to register for polaris client...");
			return;
		}
		registration.customize();
		String serviceId = registration.getServiceId();
		MetadataContext.setLocalService(serviceId);

		// Register instance.
		InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
		instanceRegisterRequest.setNamespace(polarisDiscoveryProperties.getNamespace());
		instanceRegisterRequest.setService(serviceId);
		OtUtils.setOtServiceNameIfNeeded(serviceId);
		instanceRegisterRequest.setHost(registration.getHost());
		instanceRegisterRequest.setPort(registration.getPort());
		instanceRegisterRequest.setWeight(polarisDiscoveryProperties.getWeight());
		instanceRegisterRequest.setToken(polarisDiscoveryProperties.getToken());
		instanceRegisterRequest.setRegion(staticMetadataManager.getRegion());
		instanceRegisterRequest.setZone(staticMetadataManager.getZone());
		instanceRegisterRequest.setCampus(staticMetadataManager.getCampus());
		instanceRegisterRequest.setMetadata(registration.getMetadata());
		instanceRegisterRequest.setExtendedMetadata(registration.getExtendedMetadata());
		instanceRegisterRequest.setProtocol(polarisDiscoveryProperties.getProtocol());
		instanceRegisterRequest.setVersion(polarisDiscoveryProperties.getVersion());
		instanceRegisterRequest.setInstanceId(polarisDiscoveryProperties.getInstanceId());
		CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.putMetadataStringValue(MetadataConstants.LOCAL_NAMESPACE, polarisDiscoveryProperties.getNamespace(), TransitiveType.DISPOSABLE);
		CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.putMetadataStringValue(MetadataConstants.LOCAL_SERVICE, serviceId, TransitiveType.DISPOSABLE);
		CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.putMetadataStringValue(MetadataConstants.LOCAL_IP, registration.getHost(), TransitiveType.DISPOSABLE);
		CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
				.putMetadataStringValue(MetadataConstants.LOCAL_PORT, String.valueOf(registration.getPort()), TransitiveType.DISPOSABLE);
		try {
			ProviderAPI providerClient = polarisSDKContextManager.getProviderAPI();
			InstanceRegisterResponse instanceRegisterResponse;

			// heartbeatEnabled and healthCheckUrl
			if (!polarisDiscoveryProperties.getHeartbeatEnabled()
				|| StringUtils.isBlank(polarisDiscoveryProperties.getHealthCheckUrl())) {
				// Do not enable heartbeat or configure healthCheckURL
				instanceRegisterResponse = providerClient.register(instanceRegisterRequest);
				LOGGER.info("Registered instance without heartbeat.");
			} else {
				// Enable heartbeat and configure healthCheckURL
				instanceRegisterRequest.setTtl(polarisDiscoveryProperties.getHeartbeatInterval());
				instanceRegisterResponse = providerClient.registerInstance(instanceRegisterRequest);
				LOGGER.info("Registered instance with heartbeat enabled.");
			}
			
			registration.setInstanceId(instanceRegisterResponse.getInstanceId());
			LOGGER.info("polaris registry, {} {} {} {}:{} {} {} {} {} register finished",
					instanceRegisterRequest.getNamespace(),
					instanceRegisterRequest.getService(),
					instanceRegisterResponse.getInstanceId(),
					instanceRegisterRequest.getHost(),
					instanceRegisterRequest.getPort(),
					instanceRegisterRequest.getRegion(),
					instanceRegisterRequest.getZone(),
					instanceRegisterRequest.getCampus(),
					instanceRegisterRequest.getMetadata());

			// Start stat server and configure service
			if (polarisStatProperties.isEnabled()) {
				startStatServer();
			}

			ServiceConfigImpl serviceConfig = (ServiceConfigImpl) polarisSDKContextManager.getSDKContext().getConfig()
					.getProvider().getService();
			serviceConfig.setNamespace(polarisDiscoveryProperties.getNamespace());
			serviceConfig.setName(serviceId);

			PolarisSDKContextManager.isRegistered = true;
		}
		catch (Exception e) {
			LOGGER.error("ERR_POLARIS_REGISTER, register failed...{},", registration, e);
		}
	}

	private void startStatServer() {
		try {
			StatReporter statReporter = (StatReporter) polarisSDKContextManager.getSDKContext().getPlugins()
					.getPlugin(PluginTypes.STAT_REPORTER.getBaseType(), StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS);
			if (Objects.nonNull(statReporter)) {
				ReporterMetaInfo reporterMetaInfo = statReporter.metaInfo();
				if (reporterMetaInfo.getPort() != null) {
					LOGGER.info("Stat server started on port: " + reporterMetaInfo.getPort() + " (http)");
				}
				else {
					LOGGER.info("Stat server is set to type of Push gateway");
				}
			}
			else {
				LOGGER.warn("Plugin StatReporter not found");
			}
		}
		catch (Exception e) {
			LOGGER.warn("Stat server started error, ", e);
		}
	}

	@Override
	public void deregister(PolarisRegistration registration) {
		LOGGER.info("De-registering from Polaris Server now...");

		if (StringUtils.isEmpty(registration.getServiceId()) || !PolarisSDKContextManager.isRegistered) {
			LOGGER.warn("No dom to de-register for polaris client...");
			return;
		}

		InstanceDeregisterRequest deRegisterRequest = new InstanceDeregisterRequest();
		deRegisterRequest.setInstanceID(registration.getInstanceId());
		deRegisterRequest.setToken(polarisDiscoveryProperties.getToken());
		deRegisterRequest.setNamespace(polarisDiscoveryProperties.getNamespace());
		deRegisterRequest.setService(registration.getServiceId());
		deRegisterRequest.setHost(registration.getHost());
		deRegisterRequest.setPort(registration.getPort());

		try {
			ProviderAPI providerClient = polarisSDKContextManager.getProviderAPI();
			providerClient.deRegister(deRegisterRequest);
			PolarisSDKContextManager.isRegistered = false;
			LOGGER.info("De-registration finished.");
		}
		catch (Exception e) {
			LOGGER.error("ERR_POLARIS_DEREGISTER, de-register failed...{},", registration, e);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public void setStatus(PolarisRegistration registration, String status) {
	}

	@Override
	public Object getStatus(PolarisRegistration registration) {
		String serviceName = registration.getServiceId();
		InstancesResponse instancesResponse = polarisDiscoveryHandler.getInstances(serviceName);
		Instance[] instances = instancesResponse.getInstances();
		if (null == instances) {
			return "DOWN";
		}
		for (Instance instance : instances) {
			if (instance.getHost().equalsIgnoreCase(registration.getHost())
					&& instance.getPort() == registration.getPort()) {
				return instance.isHealthy() ? "UP" : "DOWN";
			}
		}
		return "DOWN";
	}

	@Override
	public void destroy() {
	}
}
