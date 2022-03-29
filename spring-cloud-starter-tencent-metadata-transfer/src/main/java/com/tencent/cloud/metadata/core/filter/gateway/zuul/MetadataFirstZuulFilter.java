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
package com.tencent.cloud.metadata.core.filter.gateway.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Zuul output first filter used for setting peer info in context.
 *
 * @author Haotian Zhang
 */
public class MetadataFirstZuulFilter extends ZuulFilter {

	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return PRE_DECORATION_FILTER_ORDER + 1;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		// get request context
		RequestContext requestContext = RequestContext.getCurrentContext();

		// TODO The peer namespace is temporarily the same as the local namespace
		MetadataContextHolder.get().putSystemMetadata(
				MetadataConstant.SystemMetadataKey.PEER_NAMESPACE,
				MetadataContextHolder.get().getSystemMetadata(
						MetadataConstant.SystemMetadataKey.LOCAL_NAMESPACE));
		MetadataContextHolder.get().putSystemMetadata(
				MetadataConstant.SystemMetadataKey.PEER_SERVICE,
				(String) requestContext.get(SERVICE_ID_KEY));
		MetadataContextHolder.get().putSystemMetadata(
				MetadataConstant.SystemMetadataKey.PEER_PATH,
				(String) requestContext.get(REQUEST_URI_KEY));
		return null;
	}

}
