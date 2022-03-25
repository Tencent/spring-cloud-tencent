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
 */

package com.tencent.cloud.polaris.registry;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.tencent.cloud.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.polaris.PolarisProperties;
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
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.util.StringUtils;

import static org.springframework.util.ReflectionUtils.rethrowRuntimeException;

/**
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisServiceRegistry implements ServiceRegistry<Registration> {

	private static final Logger log = LoggerFactory.getLogger(PolarisServiceRegistry.class);

	private static final int ttl = 5;

	private final PolarisProperties polarisProperties;

	private final PolarisDiscoveryHandler polarisDiscoveryHandler;

	private final MetadataLocalProperties metadataLocalProperties;

	private final ScheduledExecutorService heartbeatExecutor;

	public PolarisServiceRegistry(PolarisProperties polarisProperties, PolarisDiscoveryHandler polarisDiscoveryHandler,
			MetadataLocalProperties metadataLocalProperties) {
		this.polarisProperties = polarisProperties;
		this.polarisDiscoveryHandler = polarisDiscoveryHandler;
		this.metadataLocalProperties = metadataLocalProperties;
		if (polarisProperties.isHeartbeatEnabled()) {
			ScheduledThreadPoolExecutor heartbeatExecutor = new ScheduledThreadPoolExecutor(0,
					new NamedThreadFactory("spring-cloud-heartbeat"));
			heartbeatExecutor.setMaximumPoolSize(1);
			this.heartbeatExecutor = heartbeatExecutor;
		}
		else {
			this.heartbeatExecutor = null;
		}
	}

	@Override
	public void register(Registration registration) {

		if (StringUtils.isEmpty(registration.getServiceId())) {
			log.warn("No service to register for polaris client...");
			return;
		}
		// 注册实例
		InstanceRegisterRequest instanceRegisterRequest = new InstanceRegisterRequest();
		instanceRegisterRequest.setNamespace(polarisProperties.getNamespace());
		instanceRegisterRequest.setService(registration.getServiceId());
		instanceRegisterRequest.setHost(registration.getHost());
		instanceRegisterRequest.setPort(registration.getPort());
		instanceRegisterRequest.setToken(polarisProperties.getToken());
		if (null != heartbeatExecutor) {
			instanceRegisterRequest.setTtl(ttl);
		}
		instanceRegisterRequest.setMetadata(metadataLocalProperties.getContent());
		instanceRegisterRequest.setProtocol(polarisProperties.getProtocol());
		instanceRegisterRequest.setVersion(polarisProperties.getVersion());
		try {
			ProviderAPI providerClient = polarisDiscoveryHandler.getProviderAPI();
			providerClient.register(instanceRegisterRequest);
			log.info("polaris registry, {} {} {}:{} {} register finished",
					polarisProperties.getNamespace(),
					registration.getServiceId(), registration.getHost(),
					registration.getPort(), metadataLocalProperties.getContent());

			if (null != heartbeatExecutor) {
				InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
				BeanUtils.copyProperties(instanceRegisterRequest, heartbeatRequest);
				//注册成功后开始启动心跳线程
				heartbeat(heartbeatRequest);
			}
		}
		catch (Exception e) {
			log.error("polaris registry, {} register failed...{},", registration.getServiceId(), registration, e);
			rethrowRuntimeException(e);
		}
	}

	@Override
	public void deregister(Registration registration) {

		log.info("De-registering from Polaris Server now...");

		if (StringUtils.isEmpty(registration.getServiceId())) {
			log.warn("No dom to de-register for polaris client...");
			return;
		}

		InstanceDeregisterRequest deRegisterRequest = new InstanceDeregisterRequest();
		deRegisterRequest.setToken(polarisProperties.getToken());
		deRegisterRequest.setNamespace(polarisProperties.getNamespace());
		deRegisterRequest.setService(registration.getServiceId());
		deRegisterRequest.setHost(registration.getHost());
		deRegisterRequest.setPort(registration.getPort());

		try {
			ProviderAPI providerClient = polarisDiscoveryHandler.getProviderAPI();
			providerClient.deRegister(deRegisterRequest);
		}
		catch (Exception e) {
			log.error("ERR_POLARIS_DEREGISTER, de-register failed...{},", registration, e);
		}
		finally {
			if (null != heartbeatExecutor) {
				heartbeatExecutor.shutdown();
			}
		}
		log.info("De-registration finished.");
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
					&& instance.getPort() == polarisProperties.getPort()) {
				return instance.isHealthy() ? "UP" : "DOWN";
			}
		}
		return null;
	}

	/**
	 * Start the heartbeat thread.
	 *
	 * @param heartbeatRequest heartbeat request
	 */
	public void heartbeat(InstanceHeartbeatRequest heartbeatRequest) {
		heartbeatExecutor.scheduleWithFixedDelay(() -> {
			try {
				String healthCheckEndpoint = polarisProperties.getHealthCheckUrl();
				//先判断是否配置了health-check-url，如果配置了，需要先进行服务实例健康检查，如果健康检查通过，则进行心跳上报，如果不通过，则不上报心跳
				if (Strings.isNotEmpty(healthCheckEndpoint)) {
					if (!healthCheckEndpoint.startsWith("/")) {
						healthCheckEndpoint = "/" + healthCheckEndpoint;
					}

					String healthCheckUrl = String.format("http://%s:%s%s", heartbeatRequest.getHost(), heartbeatRequest.getPort(), healthCheckEndpoint);

					if (!OkHttpUtil.get(healthCheckUrl, null)) {
						log.error("backend service health check failed. health check endpoint = {}", healthCheckEndpoint);
						return;
					}
				}

				polarisDiscoveryHandler.getProviderAPI().heartbeat(heartbeatRequest);
			}
			catch (PolarisException e) {
				log.error("polaris heartbeat[{}]", e.getCode(), e);
			}
			catch (Exception e) {
				log.error("polaris heartbeat runtime error", e);
			}
		}, 0, ttl, TimeUnit.SECONDS);
	}

}
