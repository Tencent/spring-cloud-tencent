/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 *  Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/BSD-3-Clause
 *
 *  Unless required by applicable law or agreed to in writing, software distributed
 *  under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.tencent.cloud.polaris.config.exceptions.PolarisConfigException;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;

/**
 *@author : wh
 *@date : 2022/6/28 09:30
 *@description:
 */
public class SpringInjector {
	private static volatile Injector s_injector;
	private static final Object lock = new Object();

	private static Injector getInjector() {
		if (s_injector == null) {
			synchronized (lock) {
				if (s_injector == null) {
					try {
						s_injector = Guice.createInjector(new SpringModule());
					}
					catch (Throwable ex) {
						PolarisConfigException exception = new PolarisConfigException("Unable to initialize Apollo Spring Injector!", ex);
						throw exception;
					}
				}
			}
		}

		return s_injector;
	}

	public static <T> T getInstance(Class<T> clazz) {
		try {
			return getInjector().getInstance(clazz);
		}
		catch (Throwable ex) {
			throw new PolarisConfigException(
					String.format("Unable to load instance for %s!", clazz.getName()), ex);
		}
	}

	private static class SpringModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(PlaceholderHelper.class).in(Singleton.class);
//			bind(ConfigPropertySourceFactory.class).in(Singleton.class);
			bind(SpringValueRegistry.class).in(Singleton.class);
		}
	}
}
