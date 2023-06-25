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

package com.tencent.cloud.rpc.enhancement.plugin;


import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Context used by EnhancedPlugin.
 *
 * @author Haotian Zhang
 */
public class EnhancedPluginContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedPluginContext.class);

	private EnhancedRequestContext request;

	private EnhancedResponseContext response;

	private Throwable throwable;

	private long delay;

	private ServiceInstance localServiceInstance;

	/**
	 * targetServiceInstance only exist in a client runner type.
	 */
	private ServiceInstance targetServiceInstance;

	public EnhancedRequestContext getRequest() {
		return request;
	}

	public void setRequest(EnhancedRequestContext request) {
		this.request = request;
	}

	public EnhancedResponseContext getResponse() {
		return response;
	}

	public void setResponse(EnhancedResponseContext response) {
		this.response = response;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public ServiceInstance getLocalServiceInstance() {
		return localServiceInstance;
	}

	public void setLocalServiceInstance(ServiceInstance localServiceInstance) {
		this.localServiceInstance = localServiceInstance;
	}

	public ServiceInstance getTargetServiceInstance() {
		return targetServiceInstance;
	}

	public void setTargetServiceInstance(ServiceInstance targetServiceInstance, URI url) {
		if (Objects.nonNull(targetServiceInstance)) {
			this.targetServiceInstance = targetServiceInstance;
		}
		else if (Objects.nonNull(url)) {
			DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance();
			defaultServiceInstance.setUri(url);
			if (defaultServiceInstance.isSecure()) {
				defaultServiceInstance.setPort(443);
			}
			else {
				defaultServiceInstance.setPort(80);
			}
			this.targetServiceInstance = defaultServiceInstance;
		}
		else {
			this.targetServiceInstance = new DefaultServiceInstance();
			LOGGER.warn("TargetServiceInstance is empty.");
		}
	}

	@Override
	public String toString() {
		return "EnhancedPluginContext{" +
				"request=" + request +
				", response=" + response +
				", throwable=" + throwable +
				", delay=" + delay +
				", localServiceInstance=" + localServiceInstance +
				", targetServiceInstance=" + targetServiceInstance +
				'}';
	}

}
