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

package com.tencent.cloud.polaris.eager.instrument.feign;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import com.tencent.cloud.polaris.eager.instrument.loadbalancer.LoadBalancerEagerLoadProperties;
import com.tencent.cloud.polaris.eager.instrument.loadbalancer.LoadBalancerWarmUpUtils;
import com.tencent.polaris.api.utils.StringUtils;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.JdkDynamicAopProxyUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Feign eager load context initializer.
 * Implements ApplicationListener&lt;ApplicationReadyEvent&gt; to warm up FeignClient services
 * after the application is ready.
 *
 * @author Yuwei Fu
 */
public class FeignEagerLoadContextInitializer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(FeignEagerLoadContextInitializer.class);

	private final ApplicationContext applicationContext;

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private final LoadBalancerEagerLoadProperties loadBalancerEagerLoadProperties;

	public FeignEagerLoadContextInitializer(ApplicationContext applicationContext,
			LoadBalancerClientFactory loadBalancerClientFactory,
			LoadBalancerEagerLoadProperties loadBalancerEagerLoadProperties) {
		this.applicationContext = applicationContext;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.loadBalancerEagerLoadProperties = loadBalancerEagerLoadProperties;
	}

	public static Target.HardCodedTarget<?> getHardCodedTarget(Object proxy) {
		try {
			int count = 0;
			Object invocationHandler = proxy;
			// Avoid infinite loop
			while (count++ < 100) {
				invocationHandler = Proxy.getInvocationHandler(invocationHandler);
				if (invocationHandler instanceof AopProxy) {
					invocationHandler = JdkDynamicAopProxyUtils.getTarget(invocationHandler);
					continue;
				}
				break;
			}

			for (Field field : invocationHandler.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Object fieldValue = field.get(invocationHandler);
				if (fieldValue instanceof Target.HardCodedTarget) {
					return (Target.HardCodedTarget<?>) fieldValue;
				}
			}
		}
		catch (Exception e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("proxy:{}, getTarget failed.", proxy, e);
			}
		}
		return null;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
		LOG.info("feign eager-load start");

		// Get services that are already warmed by LoadBalancerEagerContextInitializer
		Set<String> skipServices = getLoadBalancerEagerLoadServices();

		// Set to track already warmed services
		Set<String> warmedServices = new HashSet<>();

		// Warm up FeignClient services
		for (Object bean : applicationContext.getBeansWithAnnotation(FeignClient.class).values()) {
			try {
				if (Proxy.isProxyClass(bean.getClass())) {
					Target.HardCodedTarget<?> hardCodedTarget = getHardCodedTarget(bean);
					if (hardCodedTarget != null) {
						FeignClient feignClient = hardCodedTarget.type().getAnnotation(FeignClient.class);
						// if feignClient contains url, it doesn't need to eager load.
						if (StringUtils.isEmpty(feignClient.url())) {
							// support variables and default values.
							String url = hardCodedTarget.name();
							// refer to FeignClientFactoryBean, convert to URL, then take the host as the service name.
							if (!url.startsWith("http://") && !url.startsWith("https://")) {
								url = "http://" + url;
							}
							String serviceName = URI.create(url).getHost();

							// Skip if already warmed by LoadBalancerEagerContextInitializer
							if (skipServices.contains(serviceName)) {
								LOG.debug("[{}] skip eager-load, already configured in LoadBalancerEagerLoadProperties.clients", serviceName);
								continue;
							}

							// Skip if already warmed in this round
							if (warmedServices.contains(serviceName)) {
								LOG.debug("[{}] already warmed, skip.", serviceName);
								continue;
							}

							LOG.info("[{}] eager-load start, feign name: {}", serviceName, hardCodedTarget.name());
							LoadBalancerWarmUpUtils.warmUp(loadBalancerClientFactory, serviceName);

							warmedServices.add(serviceName);
						}
					}
				}
			}
			catch (Exception e) {
				LOG.debug("[{}] eager-load failed.", bean, e);
			}
		}
		LOG.info("feign eager-load end");
	}

	/**
	 * Get services configured in LoadBalancerEagerLoadProperties.
	 * These services are warmed by LoadBalancerEagerContextInitializer.
	 * @return set of service names to skip
	 */
	private Set<String> getLoadBalancerEagerLoadServices() {
		Set<String> services = new HashSet<>();
		if (loadBalancerEagerLoadProperties != null
				&& loadBalancerEagerLoadProperties.isEnabled()
				&& loadBalancerEagerLoadProperties.getClients() != null) {
			services.addAll(loadBalancerEagerLoadProperties.getClients());
		}
		return services;
	}
}
