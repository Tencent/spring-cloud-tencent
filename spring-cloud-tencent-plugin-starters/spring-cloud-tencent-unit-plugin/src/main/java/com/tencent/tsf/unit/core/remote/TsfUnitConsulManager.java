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

package com.tencent.tsf.unit.core.remote;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ecwid.consul.v1.ConsulClient;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.tsf.unit.core.Env;

public final class TsfUnitConsulManager {

	protected static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

	// 只需要 3 个长轮训
	private final static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("tsf-unit-consul-" + POOL_SEQ.getAndIncrement());
			t.setDaemon(true); // 设置为守护线程
			return t;
		}
	});
	private static ConsulClient consulClient = null;

	private static volatile boolean init = false;

	private TsfUnitConsulManager() {
	}

	public static synchronized void init() {
		if (!init) {
			init = true;
			consulClient = new ConsulClient(IPAddressUtils.getIpCompatible(Env.getConsulHost()), Env.getConsulPort());

			// 初始化先同步拉取一次
			TencentUnitArchKVLoader.syncUnitArch();
			TencentUnitRouteRuleKVLoader.syncUnitRouteRule();
			TencentUnitGrayKVLoader.syncUnitGray();
			// 启动长轮训定时任务
			executorService.scheduleAtFixedRate(TencentUnitArchKVLoader::syncUnitArch, 55, 1, TimeUnit.SECONDS);
			executorService.scheduleAtFixedRate(TencentUnitRouteRuleKVLoader::syncUnitRouteRule, 55, 1, TimeUnit.SECONDS);
			executorService.scheduleAtFixedRate(TencentUnitGrayKVLoader::syncUnitGray, 55, 1, TimeUnit.SECONDS);
		}
	}

	public static ConsulClient getConsulClient() {
		return consulClient;
	}
}
