/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
