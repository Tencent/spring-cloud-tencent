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

package com.tencent.tsf.unit.core;

import java.util.Iterator;
import java.util.ServiceLoader;

import com.tencent.tsf.unit.core.mapping.api.IMappingService;
import com.tencent.tsf.unit.core.mapping.impl.CustomerMappingService;

/**
 * 找到MappingService的实现.
 */
public final class MappingServiceLoader {

	private MappingServiceLoader() {
	}

	private static IMappingService service;

	static {
		ServiceLoader<IMappingService> mappingServices = ServiceLoader.load(IMappingService.class);
		if (mappingServices != null) {
			Iterator<IMappingService> itr = mappingServices.iterator();
			while (itr.hasNext()) {
				service = itr.next();
			}
		}

		// 默认实现
		if (service == null) {
			service = new CustomerMappingService();
		}
	}

	public static IMappingService getService() {
		return service;
	}
}
