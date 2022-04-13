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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.common.constant.MetadataConstant.SystemMetadataKey;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;
import org.apache.commons.lang.StringUtils;

import static feign.Util.checkNotNull;

/**
 * Wrap for {@link Client}.
 *
 * @author Haotian Zhang
 */
public class PolarisFeignClient implements Client {

	private final Client delegate;

	private final ConsumerAPI consumerAPI;

	public PolarisFeignClient(Client target, ConsumerAPI consumerAPI) {
		this.delegate = checkNotNull(target, "target");
		this.consumerAPI = checkNotNull(consumerAPI, "CircuitBreakAPI");
	}

	@Override
	public Response execute(Request request, Options options) throws IOException {
		final ServiceCallResult resultRequest = createServiceCallResult(request);
		try {
			Response response = delegate.execute(request, options);
			// HTTP code greater than 500 is an exception
			if (response.status() >= 500) {
				resultRequest.setRetStatus(RetStatus.RetFail);
			}
			return response;
		}
		catch (IOException origin) {
			resultRequest.setRetStatus(RetStatus.RetFail);
			throw origin;
		}
		finally {
			consumerAPI.updateServiceCallResult(resultRequest);
		}
	}

	private ServiceCallResult createServiceCallResult(final Request request) {
		ServiceCallResult resultRequest = new ServiceCallResult();

		MetadataContext metadataContext = MetadataContextHolder.get();
		String namespace = metadataContext
				.getSystemMetadata(SystemMetadataKey.PEER_NAMESPACE);
		resultRequest.setNamespace(namespace);
		String serviceName = metadataContext
				.getSystemMetadata(SystemMetadataKey.PEER_SERVICE);
		resultRequest.setService(serviceName);
		String method = metadataContext.getSystemMetadata(SystemMetadataKey.PEER_PATH);
		resultRequest.setMethod(method);
		resultRequest.setRetStatus(RetStatus.RetSuccess);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace)
				&& StringUtils.isNotBlank(sourceService)) {
			resultRequest
					.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		URI uri = URI.create(request.url());
		resultRequest.setHost(uri.getHost());
		resultRequest.setPort(uri.getPort());

		return resultRequest;
	}

}
