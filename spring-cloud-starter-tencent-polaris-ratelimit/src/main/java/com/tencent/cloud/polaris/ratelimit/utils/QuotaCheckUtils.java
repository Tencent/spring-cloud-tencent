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

package com.tencent.cloud.polaris.ratelimit.utils;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for quota checking.
 *
 * @author Haotian Zhang
 */
public final class QuotaCheckUtils {

	private static final Logger LOG = LoggerFactory.getLogger(QuotaCheckUtils.class);

	private QuotaCheckUtils() {
	}

	public static QuotaResponse getQuota(LimitAPI limitAPI, String namespace, String service, int count, String method) {
		// build quota request
		QuotaRequest quotaRequest = new QuotaRequest();
		quotaRequest.setNamespace(namespace);
		quotaRequest.setService(service);
		quotaRequest.setCount(count);
		quotaRequest.setMetadataContext(MetadataContextHolder.get());
		quotaRequest.setMethod(method);

		try {
			return limitAPI.getQuota(quotaRequest);
		}
		catch (Throwable throwable) {
			LOG.error("fail to invoke getQuota of LimitAPI with QuotaRequest[{}].", quotaRequest, throwable);
			return new QuotaResponse(new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "get quota failed"));
		}
	}
}
