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

package com.tencent.cloud.rpc.enhancement.transformer;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.Instance;

import org.springframework.cloud.client.serviceregistry.Registration;


/**
 * RegistrationTransformer extensions to adapt 3rd registration to polaris instance.
 *
 * @author andrew shan
 */
public interface RegistrationTransformer {

	String getRegistry();

	default Instance transform(Registration registration) {
		DefaultInstance instance = new DefaultInstance();
		transformDefault(instance, registration);
		transformCustom(instance, registration);
		return instance;
	}

	default void transformDefault(DefaultInstance instance, Registration registration) {
		instance.setRegistry(getRegistry());
		instance.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		instance.setService(registration.getServiceId());
		instance.setProtocol(registration.getScheme());
		instance.setId(registration.getInstanceId());
		instance.setHost(registration.getHost());
		instance.setPort(registration.getPort());
		instance.setMetadata(registration.getMetadata());
	}

	default void transformCustom(DefaultInstance instance, Registration registration) {

	}
}
