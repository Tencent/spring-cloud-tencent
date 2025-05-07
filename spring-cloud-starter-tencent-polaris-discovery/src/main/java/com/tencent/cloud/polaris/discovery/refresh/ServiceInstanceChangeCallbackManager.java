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

package com.tencent.cloud.polaris.discovery.refresh;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.reactive.PolarisReactiveDiscoveryClient;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.annotation.NonNull;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;

/**
 * Call back manager for service instance change.
 *
 * @author Haotian Zhang
 */
public class ServiceInstanceChangeCallbackManager implements ApplicationListener<ApplicationReadyEvent>, BeanPostProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(ServiceInstanceChangeCallbackManager.class);

	private final ConcurrentHashMap<String, List<ServiceInstanceChangeCallback>> callbackMap = new ConcurrentHashMap<>();

	private final ScheduledThreadPoolExecutor serviceChangeListenerExecutor;

	public ServiceInstanceChangeCallbackManager() {
		this.serviceChangeListenerExecutor = new ScheduledThreadPoolExecutor(4, new NamedThreadFactory("service-change-listener"));
	}

	public void handle(String serviceName, List<Instance> oldInstances, List<Instance> newInstances) {

		List<Instance> addInstances = new ArrayList<>();
		List<Instance> deleteInstances = new ArrayList<>();

		// calculate add instances.
		for (Instance instance : newInstances) {
			if (!oldInstances.contains(instance)) {
				addInstances.add(instance);
			}
		}
		// calculate delete instances.
		for (Instance instance : oldInstances) {
			if (!newInstances.contains(instance)) {
				deleteInstances.add(instance);
			}
		}

		if ((!CollectionUtils.isEmpty(addInstances) || !CollectionUtils.isEmpty(deleteInstances))
				&& callbackMap.containsKey(serviceName)) {
			List<ServiceInstanceChangeCallback> callbacks = callbackMap.get(serviceName);

			for (ServiceInstanceChangeCallback callback : callbacks) {
				serviceChangeListenerExecutor.execute(() -> {
					try {
						callback.callback(newInstances, addInstances, deleteInstances);
					}
					catch (Exception e) {
						LOG.error("exception in callback, service name:{}, ", serviceName, e);
					}
				});
			}
		}
	}

	@Override
	public synchronized Object postProcessAfterInitialization(Object bean, String beanName) {
		Class<?> clz = bean.getClass();

		if (!ServiceInstanceChangeCallback.class.isAssignableFrom(clz)) {
			return bean;
		}

		String serviceName = null;
		if (clz.isAnnotationPresent(ServiceInstanceChangeListener.class)) {
			ServiceInstanceChangeListener serviceInstanceChangeListener = clz.getAnnotation(ServiceInstanceChangeListener.class);
			serviceName = serviceInstanceChangeListener.serviceName();
		}

		if (StringUtils.isBlank(serviceName)) {
			return bean;
		}

		// process callback
		if (callbackMap.containsKey(serviceName)) {
			List<ServiceInstanceChangeCallback> callbacks = callbackMap.get(serviceName);
			callbacks.add((ServiceInstanceChangeCallback) bean);
		}
		else {
			List<ServiceInstanceChangeCallback> callbacks = new ArrayList<>();
			callbacks.add((ServiceInstanceChangeCallback) bean);
			callbackMap.put(serviceName, callbacks);
		}

		return bean;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
		PolarisDiscoveryClient polarisDiscoveryClient = ApplicationContextAwareUtils.getBeanIfExists(PolarisDiscoveryClient.class);
		PolarisReactiveDiscoveryClient polarisReactiveDiscoveryClient = ApplicationContextAwareUtils.getBeanIfExists(PolarisReactiveDiscoveryClient.class);
		for (String serviceName : callbackMap.keySet()) {
			try {
				if (polarisDiscoveryClient != null) {
					polarisDiscoveryClient.getInstances(serviceName);
				}
				else if (polarisReactiveDiscoveryClient != null) {
					polarisReactiveDiscoveryClient.getInstances(serviceName).subscribe();
				}
				else {
					LOG.warn("[{}] no discovery client found.", serviceName);
				}
			}
			catch (Throwable throwable) {
				LOG.error("Get instances of service [{}] failed.", serviceName, throwable);
			}
		}
	}
}
