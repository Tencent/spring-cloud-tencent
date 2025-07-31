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

package com.tencent.cloud.quickstart.callee.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.plugin.faulttolerance.annotation.FaultTolerance;
import com.tencent.cloud.plugin.faulttolerance.model.FaultToleranceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Service for fault tolerance.
 *
 * @author Haotian Zhang
 */
@Service
public class FaultToleranceService {

	private static final Logger LOG = LoggerFactory.getLogger(FaultToleranceService.class);

	private final AtomicInteger failOverCount = new AtomicInteger(0);
	private final AtomicInteger forkingCount = new AtomicInteger(0);

	@FaultTolerance(strategy = FaultToleranceStrategy.FAIL_FAST, fallbackMethod = "fallback")
	public String failFast() {
		LOG.info("Test failFast");
		throw new RuntimeException("NO");
	}

	public String fallback() {
		return "fallback success";
	}

	@FaultTolerance(strategy = FaultToleranceStrategy.FAIL_OVER, maxAttempts = 3)
	public String failOver() {
		LOG.info("Test failOver");
		if (failOverCount.incrementAndGet() % 4 == 0) {
			return "failOver success,failOverCount=" + failOverCount.get();
		}
		throw new RuntimeException("NO");
	}

	@FaultTolerance(strategy = FaultToleranceStrategy.FORKING, parallelism = 4)
	public String forking() {
		LOG.info("Test forking");
		if (forkingCount.incrementAndGet() % 4 == 0) {
			return "forking success,forkingCount=" + forkingCount.get();
		}
		throw new RuntimeException("NO");
	}

	@FaultTolerance(strategy = FaultToleranceStrategy.FAIL_OVER, maxAttempts = 3, fallbackMethod = "fallback",
			ignoreExceptions = {RuntimeException.class}, raisedExceptions = {Exception.class})
	public String raisedException(String exceptionType) throws IOException, RuntimeException, TimeoutException {
		switch (exceptionType) {
			case "RuntimeException":
				LOG.info("Test failOver for raised RuntimeException");
				throw new RuntimeException("NO");
			case "TimeOutException":
				LOG.info("Test failOver for raised TimeOutException");
				if (failOverCount.incrementAndGet() % 4 == 0) {
					return "failOver success,failOverCount=" + failOverCount.get();
				}
				throw new TimeoutException("NO");
			default:
				LOG.info("Test failOver for raised otherException");
				if (failOverCount.incrementAndGet() % 4 == 0) {
					return "failOver success,failOverCount=" + failOverCount.get();
				}
				throw new IOException("NO");
		}
	}

}
