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

package com.tencent.cloud.polaris.router.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.metadata.provider.ServletMetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;

import org.springframework.core.Ordered;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Interceptor used for setting Zuul metadata provider.
 *
 * @author Shedfree Wu
 */
public class RouterLabelZuulFilter extends ZuulFilter {
	@Override
	public String filterType() {
		return PRE_TYPE;
	}

	@Override
	public int filterOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext context = RequestContext.getCurrentContext();

		MetadataContextHolder.get().getMetadataContainer(MetadataType.MESSAGE, false)
				.setMetadataProvider(new ServletMetadataProvider(context.getRequest(),
						CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
								.getRawMetadataStringValue(MetadataConstants.LOCAL_IP)));

		return null;
	}
}
