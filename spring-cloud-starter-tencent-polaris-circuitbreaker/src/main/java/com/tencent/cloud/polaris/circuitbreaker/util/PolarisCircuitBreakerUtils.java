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

package com.tencent.cloud.polaris.circuitbreaker.util;

import java.util.Objects;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.circuitbreaker.common.PolarisCircuitBreakerConfigBuilder;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

/**
 * PolarisCircuitBreakerUtils.
 *
 * @author seanyu 2023-02-27
 */
public final class PolarisCircuitBreakerUtils {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisCircuitBreakerUtils.class);

	private PolarisCircuitBreakerUtils() {

	}

	/**
	 *
	 * @param id CircuitBreakerId
	 *              Format: namespace#service#method or service#method or service ,
	 *              namespace set as default spring.cloud.polaris.namespace if absent
	 * @return String[]{namespace, service, method}
	 */
	public static String[] resolveCircuitBreakerId(String id) {
		Assert.hasText(id, "A CircuitBreaker must have an id. Id could be : namespace#service#method or service#method or service");
		String[] polarisCircuitBreakerMetaData = id.split("#");
		if (polarisCircuitBreakerMetaData.length == 2) {
			return new String[] {MetadataContext.LOCAL_NAMESPACE, polarisCircuitBreakerMetaData[0], polarisCircuitBreakerMetaData[1]};
		}
		if (polarisCircuitBreakerMetaData.length == 3) {
			return new String[] {polarisCircuitBreakerMetaData[0], polarisCircuitBreakerMetaData[1], polarisCircuitBreakerMetaData[2]};
		}
		return new String[] {MetadataContext.LOCAL_NAMESPACE, id, ""};
	}

	public static void reportStatus(ConsumerAPI consumerAPI,
			PolarisCircuitBreakerConfigBuilder.PolarisCircuitBreakerConfiguration conf, CallAbortedException e) {
		try {
			ServiceCallResult result = new ServiceCallResult();
			result.setMethod(conf.getMethod());
			result.setNamespace(conf.getNamespace());
			result.setService(conf.getService());
			result.setRuleName(e.getRuleName());
			result.setRetStatus(RetStatus.RetReject);
			result.setCallerService(new ServiceKey(conf.getSourceNamespace(), conf.getSourceService()));

			if (Objects.nonNull(e.getFallbackInfo())) {
				result.setRetCode(e.getFallbackInfo().getCode());
			}
			consumerAPI.updateServiceCallResult(result);
		}
		catch (Throwable ex) {
			LOG.error("[CircuitBreaker] report circuitbreaker call result fail ", ex);
		}
	}

}
