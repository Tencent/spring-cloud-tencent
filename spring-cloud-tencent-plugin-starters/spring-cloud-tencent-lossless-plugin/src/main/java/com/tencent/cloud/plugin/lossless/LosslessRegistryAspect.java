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

package com.tencent.cloud.plugin.lossless;

import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.plugin.lossless.common.HttpLosslessActionProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

/**
 * Intercept for of register and deregister.
 *
 * @author Shedfree Wu
 */
@Aspect
public class LosslessRegistryAspect {

	private ServiceRegistry<Registration> serviceRegistry;

	private Registration registration;

	private LosslessProperties losslessProperties;

	private PolarisSDKContextManager polarisSDKContextManager;

	private RegistrationTransformer registrationTransformer;

	private PolarisContextProperties properties;

	public LosslessRegistryAspect(ServiceRegistry<Registration> serviceRegistry, Registration registration,
			PolarisContextProperties properties, LosslessProperties losslessProperties,
			PolarisSDKContextManager polarisSDKContextManager, RegistrationTransformer registrationTransformer) {
		this.serviceRegistry = serviceRegistry;
		this.registration = registration;
		this.losslessProperties = losslessProperties;
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.registrationTransformer = registrationTransformer;
		this.properties = properties;
	}

	@Pointcut("execution(public * org.springframework.cloud.client.serviceregistry.ServiceRegistry.register(..))")
	public void registerPointcut() {

	}

	@Pointcut("execution(public * org.springframework.cloud.client.serviceregistry.ServiceRegistry.deregister(..))")
	public void deregisterPointcut() {

	}

	@Around("registerPointcut()")
	public Object invokeRegister(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!losslessProperties.isEnabled()) {
			return joinPoint.proceed();
		}

		// web started, get port from registration
		BaseInstance instance = getBaseInstance(registration, registrationTransformer);

		Runnable registerAction = () -> executeJoinPoint(joinPoint);
		Runnable deregisterAction = () -> serviceRegistry.deregister(registration);
		HttpLosslessActionProvider losslessActionProvider =
				new HttpLosslessActionProvider(registerAction, deregisterAction, registration.getPort(),
						instance, polarisSDKContextManager.getSDKContext().getExtensions());

		polarisSDKContextManager.getLosslessAPI().setLosslessActionProvider(instance, losslessActionProvider);
		polarisSDKContextManager.getLosslessAPI().losslessRegister(instance);
		// return void
		return null;
	}

	@Around("deregisterPointcut()")
	public Object invokeDeregister(ProceedingJoinPoint joinPoint) throws Throwable {
		return joinPoint.proceed();
	}

	public void executeJoinPoint(ProceedingJoinPoint joinPoint) {
		try {
			joinPoint.proceed();
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static BaseInstance getBaseInstance(Registration registration, RegistrationTransformer registrationTransformer) {
		return registrationTransformer.transform(registration);
	}
}
