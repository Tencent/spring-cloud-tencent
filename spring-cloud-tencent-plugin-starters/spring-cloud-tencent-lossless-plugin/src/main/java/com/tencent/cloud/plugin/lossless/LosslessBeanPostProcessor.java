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

package com.tencent.cloud.plugin.lossless;

import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.DefaultBaseInstance;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

/**
 * Wrap Spring Bean and proxy for serviceRegistry.
 *
 * @author Shedfree Wu
 */
public class LosslessBeanPostProcessor implements BeanPostProcessor {

	private PolarisSDKContextManager polarisSDKContextManager;

	private LosslessProperties losslessProperties;

	private Registration registration;

	private Integer port;

	public LosslessBeanPostProcessor(PolarisSDKContextManager polarisSDKContextManager,
			LosslessProperties losslessProperties, Registration registration, Integer port) {
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.losslessProperties = losslessProperties;
		this.registration = registration;
		this.port = port;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AbstractAutoServiceRegistration) {
			wrap(bean, polarisSDKContextManager, losslessProperties, registration, port);
		}
		return bean;
	}

	public static void wrap(Object bean, PolarisSDKContextManager polarisSDKContextManager,
			LosslessProperties losslessProperties, Registration registration, Integer port) {
		LosslessProxyServiceRegistry proxyServiceRegistry;
		String clsName = bean.getClass().getCanonicalName();
		if (clsName.contains("org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration")) {
			ServiceRegistry<Registration> registry =
					(ServiceRegistry<Registration>) ReflectionUtils.
							getObjectByFieldName(bean, "serviceRegistry");
			proxyServiceRegistry = new LosslessProxyServiceRegistry(registry, polarisSDKContextManager, registration);
			ReflectionUtils.setValueByFieldName(bean, "serviceRegistry", proxyServiceRegistry);
		}
		else {
			ServiceRegistry<Registration> registry =
					(ServiceRegistry<Registration>) ReflectionUtils.
							getSuperObjectByFieldName(bean, "serviceRegistry");
			proxyServiceRegistry = new LosslessProxyServiceRegistry(registry, polarisSDKContextManager, registration);
			ReflectionUtils.setSuperValueByFieldName(bean, "serviceRegistry", proxyServiceRegistry);
		}
		SpringCloudLosslessActionProvider losslessActionProvider =
				new SpringCloudLosslessActionProvider(proxyServiceRegistry, losslessProperties);
		polarisSDKContextManager.getLosslessAPI().setLosslessActionProvider(
				getBaseInstance(registration, port), losslessActionProvider);
	}

	public static BaseInstance getBaseInstance(Registration registration, Integer port) {
		// registration 通用，不设置 ns
		DefaultBaseInstance baseInstance = new DefaultBaseInstance();
		baseInstance.setService(registration.getServiceId());
		// 由于 PolarisRegistration 的 port 在 web 启动后才能生成，需从外部传入
		baseInstance.setPort(port);
		baseInstance.setHost(registration.getHost());
		return baseInstance;
	}
}
