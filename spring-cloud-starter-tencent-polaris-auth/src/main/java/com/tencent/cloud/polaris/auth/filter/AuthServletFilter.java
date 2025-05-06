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

package com.tencent.cloud.polaris.auth.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.polaris.auth.utils.AuthenticateUtils;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.api.rpc.AuthResponse;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter to authenticate.
 *
 * @author Haotian Zhang
 */
@Order(OrderConstant.Server.Servlet.AUTH_FILTER_ORDER)
public class AuthServletFilter extends OncePerRequestFilter {

	/**
	 * Default Filter Registration Bean Name Defined .
	 */
	public static final String AUTH_FILTER_BEAN_NAME = "authFilterRegistrationBean";

	private final AuthAPI authAPI;

	public AuthServletFilter(AuthAPI authAPI) {
		this.authAPI = authAPI;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		AuthResponse authResponse = AuthenticateUtils.authenticate(authAPI, MetadataContext.LOCAL_NAMESPACE,
				MetadataContext.LOCAL_SERVICE, request.getRequestURI(), "HTTP", request.getMethod());
		if (authResponse != null && authResponse.getAuthResult().getCode()
				.equals(AuthResult.Code.AuthResultForbidden)) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return;
		}
		filterChain.doFilter(request, response);
	}
}
