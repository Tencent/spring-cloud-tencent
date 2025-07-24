/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
