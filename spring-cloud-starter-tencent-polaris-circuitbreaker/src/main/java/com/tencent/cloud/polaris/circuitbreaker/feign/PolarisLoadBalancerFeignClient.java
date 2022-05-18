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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import feign.Client;
import feign.Request;
import feign.Response;

import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.PEER_SERVICE;

/**
 * Wrap for {@link LoadBalancerFeignClient}.
 *
 * @author Haotian Zhang
 */
public class PolarisLoadBalancerFeignClient extends LoadBalancerFeignClient {

	public PolarisLoadBalancerFeignClient(Client delegate,
			CachingSpringLoadBalancerFactory lbClientFactory,
			SpringClientFactory clientFactory) {
		super(delegate, lbClientFactory, clientFactory);
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		Map<String, Collection<String>> headers = new HashMap<>(request.headers());
		headers.put(PEER_SERVICE, Collections.singletonList(URI.create(request.url()).getAuthority()));
		request = Request.create(request.httpMethod(), request.url(), headers, request.requestBody());
		return super.execute(request, options);
	}
}
