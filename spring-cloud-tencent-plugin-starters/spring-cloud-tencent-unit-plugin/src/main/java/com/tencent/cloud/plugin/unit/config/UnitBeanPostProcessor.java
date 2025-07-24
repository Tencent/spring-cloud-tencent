/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.cloud.plugin.unit.config;

import com.tencent.cloud.plugin.unit.discovery.UnitFeignEagerLoadSmartLifecycle;
import com.tencent.cloud.plugin.unit.discovery.UnitPolarisDiscoveryClient;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClient;
import com.tencent.cloud.polaris.eager.instrument.feign.FeignEagerLoadSmartLifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class UnitBeanPostProcessor implements BeanPostProcessor {


	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof PolarisDiscoveryClient discoveryClient) {
			return new UnitPolarisDiscoveryClient(discoveryClient);
		}

		if (bean instanceof FeignEagerLoadSmartLifecycle) {
			return new UnitFeignEagerLoadSmartLifecycle();
		}

		return bean;
	}
}
