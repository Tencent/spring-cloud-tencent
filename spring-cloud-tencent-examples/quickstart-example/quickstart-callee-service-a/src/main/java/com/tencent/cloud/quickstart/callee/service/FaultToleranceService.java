/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.plugin.faulttolerance.annotation.FaultTolerance;
import com.tencent.cloud.plugin.faulttolerance.model.FaultToleranceStrategy;

import org.springframework.stereotype.Service;

/**
 * Service for fault tolerance.
 *
 * @author Haotian Zhang
 */
@Service
public class FaultToleranceService {

	private final AtomicInteger failOverCount = new AtomicInteger(0);
	private final AtomicInteger forkingCount = new AtomicInteger(0);

	@FaultTolerance(strategy = FaultToleranceStrategy.FAIL_FAST, fallbackMethod = "fallback")
	public String failFast() {
		throw new RuntimeException("NO");
	}

	public String fallback() {
		return "fallback";
	}

	@FaultTolerance(strategy = FaultToleranceStrategy.FAIL_OVER, maxAttempts = 3)
	public String failOver() {
		if (failOverCount.incrementAndGet() % 4 == 0) {
			return "OK";
		}
		throw new RuntimeException("NO");
	}

	@FaultTolerance(strategy = FaultToleranceStrategy.FORKING, parallelism = 4)
	public String forking() {
		if (forkingCount.incrementAndGet() % 4 == 0) {
			return "OK";
		}
		throw new RuntimeException("NO");
	}
}
