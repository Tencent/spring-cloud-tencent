/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.plugin.discovery.multi.listeners;

import java.util.Set;

import com.tencent.cloud.polaris.config.annotation.PolarisConfigKVFileChangeListener;
import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.flow.RegisterStateManager;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.composite.CompositeConnector;
import com.tencent.polaris.plugins.connector.consul.ConsulAPIConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consul Discovery Config Listener .
 *
 * @author Haotian Zhang
 */
public final class ConsulDiscoveryConfigChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(ConsulDiscoveryConfigChangeListener.class);

	private final PolarisRegistration polarisRegistration;

	private final SDKContext sdkContext;
	private final ConsulAPIConnector consulAPIConnector;
	private InstanceRegisterRequest instanceRegisterRequest;

	public ConsulDiscoveryConfigChangeListener(PolarisRegistration polarisRegistration) {
		this.polarisRegistration = polarisRegistration;
		this.sdkContext = polarisRegistration.getPolarisContext();

		ServerConnector connector = (ServerConnector) sdkContext.getPlugins()
				.getPlugin(PluginTypes.SERVER_CONNECTOR.getBaseType(), sdkContext.getValueContext()
						.getServerConnectorProtocol());
		ConsulAPIConnector temp = null;
		if (connector instanceof CompositeConnector) {
			CompositeConnector compositeConnector = (CompositeConnector) connector;
			for (DestroyableServerConnector sc : compositeConnector.getServerConnectors()) {
				if (sc instanceof ConsulAPIConnector) {
					temp = (ConsulAPIConnector) sc;
					break;
				}
			}
		}
		else if (connector instanceof ConsulAPIConnector) {
			temp = (ConsulAPIConnector) connector;
		}
		this.consulAPIConnector = temp;
	}

	@PolarisConfigKVFileChangeListener(interestedKeyPrefixes = "spring.cloud.consul.discovery")
	public void onChange(ConfigChangeEvent event) {
		if (consulAPIConnector != null) {
			initInstanceRegisterRequest();

			Set<String> changedKeys = event.changedKeys();
			for (String changedKey : changedKeys) {
				if (StringUtils.equals(changedKey, "spring.cloud.consul.discovery.enabled")) {
					LOG.info("{} = {}", changedKey, event.getChange(changedKey));
					boolean discoveryEnabled = !StringUtils.equals("false", event.getChange(changedKey).getNewValue()
							.toString());
					consulAPIConnector.setDiscoveryEnable(discoveryEnabled);
				}
				else if (StringUtils.equals(changedKey, "spring.cloud.consul.discovery.register")) {
					LOG.info("{} = {}", changedKey, event.getChange(changedKey));
					boolean registerEnabled = !StringUtils.equals("false", event.getChange(changedKey).getNewValue()
							.toString());
					if (registerEnabled) {
						consulAPIConnector.registerInstance(RegisterStateManager.getRegisterState(sdkContext, instanceRegisterRequest)
								.getInstanceRegisterRequest().getRequest(), null);
					}
					else {
						consulAPIConnector.deregisterInstance(RegisterStateManager.getRegisterState(sdkContext, instanceRegisterRequest)
								.getInstanceRegisterRequest().getRequest());
					}
				}
			}
		}
	}

	private void initInstanceRegisterRequest() {
		if (instanceRegisterRequest == null) {
			this.instanceRegisterRequest = new InstanceRegisterRequest();
			instanceRegisterRequest.setNamespace(polarisRegistration.getNamespace());
			instanceRegisterRequest.setService(polarisRegistration.getServiceId());
			instanceRegisterRequest.setHost(polarisRegistration.getHost());
			instanceRegisterRequest.setPort(polarisRegistration.getPort());
		}
	}
}
