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

import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;

import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

/**
 * Lossless proxy for {@link ServiceRegistry}.
 *
 * @author Shedfree Wu
 */
public class LosslessProxyServiceRegistry implements ServiceRegistry<Registration> {

	private final ServiceRegistry<Registration> target;

	private Registration registration;

	private PolarisSDKContextManager polarisSDKContextManager;

	private final AtomicBoolean doneDeregister = new AtomicBoolean(false);

	public LosslessProxyServiceRegistry(ServiceRegistry<Registration> target,
			PolarisSDKContextManager polarisSDKContextManager, Registration registration) {
		this.target = target;
		this.polarisSDKContextManager = polarisSDKContextManager;
		this.registration = registration;
	}

	@Override
	public void register(Registration registration) {
		this.registration = registration;
		// web started, get port from registration
		polarisSDKContextManager.getLosslessAPI().losslessRegister(
				LosslessBeanPostProcessor.getBaseInstance(registration, registration.getPort()));
	}

	@Override
	public void deregister(Registration registration) {
		if (doneDeregister.compareAndSet(false, true)) {
			target.deregister(registration);
		}
	}

	public void deregister() {
		// 需要兼容其他 discovery, spring cloud deregister 统一幂等处理
		if (registration != null && doneDeregister.compareAndSet(false, true)) {
			target.deregister(registration);
		}
	}

	public ServiceRegistry<Registration> getTarget() {
		return target;
	}

	public Registration getRegistration() {
		return registration;
	}

	@Override
	public void close() {
		target.close();
	}

	@Override
	public void setStatus(Registration registration, String status) {
		target.setStatus(registration, status);
	}

	@Override
	public <T> T getStatus(Registration registration) {
		return target.getStatus(registration);
	}
}
