/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.cloud.plugin.unit.discovery;

import com.tencent.cloud.polaris.eager.instrument.feign.FeignEagerLoadSmartLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitFeignEagerLoadSmartLifecycle extends FeignEagerLoadSmartLifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(UnitFeignEagerLoadSmartLifecycle.class);


	public UnitFeignEagerLoadSmartLifecycle() {
		super(null, null, null);
	}

	@Override
	public void start() {
		LOG.info("ignore feign eager load in unit mode");
	}
}
