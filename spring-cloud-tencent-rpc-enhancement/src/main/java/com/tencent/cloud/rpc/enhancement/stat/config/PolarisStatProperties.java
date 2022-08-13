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

package com.tencent.cloud.rpc.enhancement.stat.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for stat reporter.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.stat")
public class PolarisStatProperties {

	/**
	 * If state reporter enabled.
	 */
	private boolean enabled = false;

	/**
	 * Local host for prometheus to pull.
	 */
	private String host;

	/**
	 * Port for prometheus to pull.
	 */
	private int port = 28080;

	/**
	 * Path for prometheus to pull.
	 */
	private String path = "/metrics";

	/**
	 * PushGatewayProperties.
	 */
	private PushGatewayProperties pushgateway;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public PushGatewayProperties getPushgateway() {
		return pushgateway;
	}

	public void setPushgateway(PushGatewayProperties pushgateway) {
		this.pushgateway = pushgateway;
	}

	public static class PushGatewayProperties {
		/**
		 * Enable publishing via a Prometheus pushGateway.
		 */
		private Boolean enabled = false;

		/**
		 * Required host:port or ip:port of the pushGateway.
		 */
		private String address = "localhost:9091";

		/**
		 * Required identifier for this application instance.
		 */
		private String job;

		/**
		 * Frequency with which to push metrics to pushGateway,default 1 minutes.
		 */
		private Duration pushRate = Duration.ofMinutes(1);

		/**
		 * PushGateway shutDownStrategy when application is is shut-down.
		 */
		private ShutDownStrategy shutDownStrategy;

		/**
		 * Used to group metrics in pushGateway. eg:instance:instanceName
		 */
		private Map<String, String> groupingKeys = new HashMap<>();

		public Boolean getEnabled() {
			return enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getJob() {
			return job;
		}

		public void setJob(String job) {
			this.job = job;
		}

		public Duration getPushRate() {
			return pushRate;
		}

		public void setPushRate(Duration pushRate) {
			this.pushRate = pushRate;
		}

		public Map<String, String> getGroupingKeys() {
			return groupingKeys;
		}

		public void setGroupingKeys(Map<String, String> groupingKeys) {
			this.groupingKeys = groupingKeys;
		}

		public ShutDownStrategy getShutDownStrategy() {
			return shutDownStrategy;
		}

		public void setShutDownStrategy(ShutDownStrategy shutDownStrategy) {
			this.shutDownStrategy = shutDownStrategy;
		}
	}

	/**
	 * PushGateway shutDownStrategy when application is is shut-down.
	 */
	public enum ShutDownStrategy {

		/**
		 * Delete metrics from pushGateway when application is shut-down.
		 */
		DELETE,

		/**
		 * Push metrics right before shut-down. Mostly useful for batch jobs.
		 */
		PUSH
	}
}
