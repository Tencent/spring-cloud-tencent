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

package com.tencent.cloud.rpc.enhancement.stat.config.plugin;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatProperties;
import com.tencent.polaris.client.util.NamedThreadFactory;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PrometheusPushGatewayContainer will push the metrics aggregated by
 * {@link com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandler} to Prometheus pushGateway.
 *
 * @author lingxiao.wlx
 */
public class PrometheusPushGatewayContainer {

	private final Logger logger = LoggerFactory.getLogger(PrometheusPushGatewayContainer.class);
	/**
	 * {@link com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandler} register the Collector with
	 * CollectorRegistry.defaultRegistry.
	 */
	private final CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
	private final String address;
	private final String job;
	private final PolarisStatProperties.ShutDownStrategy shutDownStrategy;
	private final PushGateway pushGateway;
	private final Map<String, String> groupingKey;

	private final ScheduledExecutorService executorService;
	private final ScheduledFuture<?> scheduledFuture;

	public PrometheusPushGatewayContainer(String address, Duration pushRate, String job,
				PolarisStatProperties.ShutDownStrategy shutDownStrategy, Map<String, String> groupingKey) {
		this.address = address;
		this.job = job;
		this.shutDownStrategy = shutDownStrategy;
		this.pushGateway = new PushGateway(address);
		this.groupingKey = groupingKey;
		this.executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("PrometheusPushGateway") {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = super.newThread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		this.scheduledFuture = executorService.scheduleAtFixedRate(() -> {
			this.push();
			if (logger.isDebugEnabled()) {
				logger.debug("push metrics to Prometheus pushGateway success!");
			}
		}, 0, pushRate.toMillis(), TimeUnit.MILLISECONDS);

	}

	/**
	 * Call by Spring to destroy PrometheusPushGatewayContainer instance.
	 */
	public void shutdown() {
		shutdown(this.shutDownStrategy);
	}

	private void shutdown(PolarisStatProperties.ShutDownStrategy shutDownStrategy) {
		executorService.shutdown();
		scheduledFuture.cancel(false);
		if (Objects.isNull(shutDownStrategy)) {
			return;
		}
		switch (shutDownStrategy) {
			case PUSH:
				push();
				break;
			case DELETE:
				delete();
				break;
		}
	}

	private void push() {
		try {
			pushGateway.pushAdd(collectorRegistry, this.job, this.groupingKey);
		}
		catch (UnknownHostException e) {
			logger.error("Unable to locate host {}. No longer attempting metrics publication to this host", this.address);
			// if cache UnknownHostException,shutdown task
			this.shutdown(null);
		}
		catch (Throwable t) {
			logger.error("Unable to push metrics to Prometheus pushGateway", t);
		}
	}

	private void delete() {
		try {
			pushGateway.delete(this.job, this.groupingKey);
		}
		catch (Throwable t) {
			logger.error("Unable to delete metrics from Prometheus pushGateway", t);
		}
	}
}
