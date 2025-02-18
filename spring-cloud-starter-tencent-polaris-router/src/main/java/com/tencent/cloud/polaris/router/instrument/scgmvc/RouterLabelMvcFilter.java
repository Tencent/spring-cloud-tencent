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

package com.tencent.cloud.polaris.router.instrument.scgmvc;

import java.io.IOException;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.metadata.provider.ServletMetadataProvider;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;

import static org.springframework.cloud.gateway.server.mvc.filter.FormFilter.FORM_FILTER_ORDER;

/**
 * Interceptor used for setting SCG MVC metadata provider.
 *
 * @author luguoji
 */
public class RouterLabelMvcFilter implements Filter, Ordered {

	@Override
	public int getOrder() {
		return FORM_FILTER_ORDER - 1;
	}

	/**
	 * Create new RequestData for passing router labels.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.MESSAGE, false)
				.setMetadataProvider(new ServletMetadataProvider((HttpServletRequest) request,
						CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer()
								.getRawMetadataStringValue(MetadataConstants.LOCAL_IP)));
		filterChain.doFilter(request, response);
	}
}
