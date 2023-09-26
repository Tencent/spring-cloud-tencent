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

package com.tencent.cloud.polaris.contract.filter;

import java.io.IOException;

import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_RESOURCE_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_UI_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V2_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V3_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V2_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V3_PREFIX;

/**
 * Filter to disable api doc controller.
 *
 * @author Haotian Zhang
 */
public class ApiDocServletFilter extends OncePerRequestFilter {

	private final PolarisContractProperties polarisContractProperties;

	public ApiDocServletFilter(PolarisContractProperties polarisContractProperties) {
		this.polarisContractProperties = polarisContractProperties;
	}

	@Override
	public void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
			@NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		if (!polarisContractProperties.isExposure()) {
			String path = httpServletRequest.getServletPath();
			if (path.equals(SWAGGER_V2_API_DOC_URL) ||
					path.startsWith(SWAGGER_V3_API_DOC_URL) ||
					path.equals(SWAGGER_UI_URL) ||
					path.startsWith(SWAGGER_RESOURCE_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V2_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V3_PREFIX)) {
				httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}
}

