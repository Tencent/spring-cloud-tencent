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

package com.tencent.cloud.plugin.tsf.tls;

import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationListener;

/**
 * Application listener for tls init.
 *
 * @author Haotian Zhang
 */
public class TlsReadyApplicationListener implements ApplicationListener<ApplicationStartedEvent> {

	private static final Logger log = LoggerFactory.getLogger(TlsReadyApplicationListener.class);

	private final AtomicBoolean isSet = new AtomicBoolean(false);

	@Override
	public void onApplicationEvent(@NotNull ApplicationStartedEvent event) {
		SslBundles sslBundles = ApplicationContextAwareUtils.getBeanIfExists(SslBundles.class, true);
		ContextRefresher contextRefresher = ApplicationContextAwareUtils.getBeanIfExists(ContextRefresher.class, true);
		try {
			if (sslBundles != null && contextRefresher != null && isSet.compareAndSet(false, true)
					&& sslBundles.getBundleNames().contains("tsf")) {
				sslBundles.addBundleUpdateHandler("tsf", sslBundle -> contextRefresher.refresh());
			}
			else if (sslBundles != null && !sslBundles.getBundleNames().contains("tsf")) {
				log.warn("tsf ssl bundle is not registered.");
			}
		}
		catch (Throwable throwable) {
			log.warn("tsf ssl bundle is not registered correctly.", throwable);
		}
	}
}
