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

package com.tencent.cloud.metadata.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.cloud.common.async.PolarisAsyncProperties;
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.common.util.TsfTagUtils;
import com.tencent.cloud.common.util.UrlUtils;
import com.tencent.cloud.metadata.provider.ServletMetadataProvider;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.APPLICATION_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static com.tencent.polaris.metadata.core.constant.MetadataConstants.LOCAL_IP;

/**
 * Filter used for storing the metadata from upstream temporarily when web application is
 * SERVLET.
 *
 * @author Haotian Zhang
 */
@Order(OrderConstant.Server.Servlet.DECODE_TRANSFER_METADATA_FILTER_ORDER)
public class DecodeTransferMetadataServletFilter extends OncePerRequestFilter {

	private static final Logger LOG = LoggerFactory.getLogger(DecodeTransferMetadataServletFilter.class);

	private final PolarisAsyncProperties polarisAsyncProperties;

	public DecodeTransferMetadataServletFilter(PolarisAsyncProperties polarisAsyncProperties) {
		this.polarisAsyncProperties = polarisAsyncProperties;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
			@NonNull HttpServletResponse httpServletResponse, FilterChain filterChain)
			throws ServletException, IOException {
		Map<String, String> mergedTransitiveMetadata = new HashMap<>();
		Map<String, String> mergedDisposableMetadata = new HashMap<>();
		Map<String, String> mergedApplicationMetadata = new HashMap<>();
		// some tsf headers need to change to polaris header
		Map<String, String> addHeaders = new HashMap<>();
		AtomicReference<String> callerIp = new AtomicReference<>("");

		TsfTagUtils.updateTsfMetadata(mergedTransitiveMetadata, mergedDisposableMetadata,
				mergedApplicationMetadata, addHeaders, callerIp,
				httpServletRequest.getHeader(MetadataConstant.HeaderName.TSF_TAGS),
				httpServletRequest.getHeader(MetadataConstant.HeaderName.TSF_SYSTEM_TAG),
				httpServletRequest.getHeader(MetadataConstant.HeaderName.TSF_METADATA));

		// transitive metadata
		// from specific header
		Map<String, String> internalTransitiveMetadata = getInternalMetadata(httpServletRequest, CUSTOM_METADATA);
		// from header with specific prefix
		Map<String, String> customTransitiveMetadata = CustomTransitiveMetadataResolver.resolve(httpServletRequest);

		mergedTransitiveMetadata.putAll(internalTransitiveMetadata);
		mergedTransitiveMetadata.putAll(customTransitiveMetadata);

		// disposable metadata
		// from specific header
		Map<String, String> internalDisposableMetadata = getInternalMetadata(httpServletRequest, CUSTOM_DISPOSABLE_METADATA);
		mergedDisposableMetadata.putAll(internalDisposableMetadata);

		// application metadata
		Map<String, String> internalApplicationMetadata = getInternalMetadata(httpServletRequest, APPLICATION_METADATA);
		mergedApplicationMetadata.putAll(internalApplicationMetadata);


		if (StringUtils.isNotBlank(mergedApplicationMetadata.get(LOCAL_IP))) {
			callerIp.set(mergedApplicationMetadata.get(LOCAL_IP));
		}
		// add headers
		httpServletRequest = new HttpServletRequestHeaderWrapper(httpServletRequest, addHeaders);
		// message metadata
		ServletMetadataProvider callerMessageMetadataProvider = new ServletMetadataProvider(httpServletRequest,
				callerIp.get(), polarisAsyncProperties.getEnabled());

		MetadataContextHolder.init(mergedTransitiveMetadata, mergedDisposableMetadata, mergedApplicationMetadata, callerMessageMetadataProvider);

		TransHeadersTransfer.transfer(httpServletRequest);
		try {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}
		finally {
			// Clean up ThreadLocal.
			MetadataContextHolder.remove();
		}
	}

	private Map<String, String> getInternalMetadata(HttpServletRequest httpServletRequest, String headerName) {
		// Get custom metadata string from http header.
		String customMetadataStr = UrlUtils.decode(httpServletRequest.getHeader(headerName));
		LOG.debug("Get upstream metadata string: {}", customMetadataStr);

		// create custom metadata.
		return JacksonUtils.deserialize2Map(customMetadataStr);
	}
}
