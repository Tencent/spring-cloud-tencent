/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.circuitbreaker;

import java.util.function.Function;
import java.util.function.Supplier;

import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisResultToErrorCode;
import com.tencent.cloud.polaris.circuitbreaker.util.PolarisCircuitBreakerUtils;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;

/**
 * PolarisCircuitBreaker.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreaker implements CircuitBreaker {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisCircuitBreaker.class);

	private final FunctionalDecorator decorator;

	private final PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf;

	private final ConsumerAPI consumerAPI;

	public PolarisCircuitBreaker(PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(new ServiceKey(conf.getNamespace(), conf.getService()), conf.getMethod());
		makeDecoratorRequest.setSourceService(new ServiceKey(conf.getSourceNamespace(), conf.getSourceService()));
		makeDecoratorRequest.setResultToErrorCode(new PolarisResultToErrorCode());
		this.consumerAPI = consumerAPI;
		this.conf = conf;
		this.decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
	}

	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		Supplier<T> toRunDecorator = decorator.decorateSupplier(toRun);
		try {
			return toRunDecorator.get();
		}
		catch (CallAbortedException e) {
			LOGGER.debug("PolarisCircuitBreaker CallAbortedException: {}", e.getMessage());
			PolarisCircuitBreakerUtils.reportStatus(consumerAPI, conf, e);
			return fallback.apply(e);
		}
		catch (Exception e) {
			return fallback.apply(e);
		}
	}

}
