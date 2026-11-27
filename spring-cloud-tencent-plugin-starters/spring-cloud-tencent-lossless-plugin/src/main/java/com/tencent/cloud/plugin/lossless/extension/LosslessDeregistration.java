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

package com.tencent.cloud.plugin.lossless.extension;

import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * Strict deregistration path used by lossless actions.
 *
 * @author evelynwei
 */
public interface LosslessDeregistration {

	/**
	 * Deregister the registration and return the outcome of the underlying invocation.
	 * @param registration registration to deregister
	 * @param deregistration callback that invokes the original ServiceRegistry deregistration path
	 * @return outcome of the strict deregistration
	 */
	LosslessDeregistrationResult deregisterForLossless(Registration registration, Runnable deregistration);
}
