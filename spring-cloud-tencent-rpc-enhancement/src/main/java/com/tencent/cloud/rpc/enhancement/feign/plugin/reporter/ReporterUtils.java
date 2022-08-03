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

package com.tencent.cloud.rpc.enhancement.feign.plugin.reporter;

import java.net.URI;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import feign.Request;
import org.apache.commons.lang.StringUtils;

/**
 * Util for polaris reporter.
 *
 * @author Haotian Zhang
 */
public final class ReporterUtils {

	private ReporterUtils() {
	}

	public static ServiceCallResult createServiceCallResult(final Request request, RetStatus retStatus) {
		ServiceCallResult resultRequest = new ServiceCallResult();

		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		String serviceName = request.requestTemplate().feignTarget().name();
		resultRequest.setService(serviceName);
		URI uri = URI.create(request.url());
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetStatus(retStatus);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		resultRequest.setHost(uri.getHost());
		resultRequest.setPort(uri.getPort());

		return resultRequest;
	}
}
