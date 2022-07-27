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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * Extend ResponseErrorHandler to get request information.
 *
 * @author wh 2022/6/21
 */
public class EnhancedRestTemplateReporter extends AbstractPolarisReporterAdapter implements ResponseErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedRestTemplateReporter.class);

	private static final String FILE_NAME = "connection";

	private final ConsumerAPI consumerAPI;

	private final PolarisResponseErrorHandler polarisResponseErrorHandler;

	public EnhancedRestTemplateReporter(RpcEnhancementReporterProperties properties, ConsumerAPI consumerAPI,
			PolarisResponseErrorHandler polarisResponseErrorHandler) {
		super(properties);
		this.consumerAPI = consumerAPI;
		this.polarisResponseErrorHandler = polarisResponseErrorHandler;
	}

	@Override
	public boolean hasError(@NonNull ClientHttpResponse response) {
		return true;
	}

	@Override
	public void handleError(@NonNull ClientHttpResponse response) throws IOException {
		if (Objects.nonNull(polarisResponseErrorHandler)) {
			if (polarisResponseErrorHandler.hasError(response)) {
				polarisResponseErrorHandler.handleError(response);
			}
		}
	}

	@Override
	public void handleError(@NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response)
			throws IOException {
		ServiceCallResult resultRequest = createServiceCallResult(url);
		try {
			HttpURLConnection connection = (HttpURLConnection) ReflectionUtils.getFieldValue(response, FILE_NAME);
			if (connection != null) {
				URL realURL = connection.getURL();
				resultRequest.setHost(realURL.getHost());
				resultRequest.setPort(realURL.getPort());
			}

			if (apply(response.getStatusCode())) {
				resultRequest.setRetStatus(RetStatus.RetFail);
			}
		}
		catch (Exception e) {
			LOG.error("Will report response of {} url {}", response, url, e);
			resultRequest.setRetStatus(RetStatus.RetFail);
			throw e;
		}
		finally {
			LOG.debug("Will report result of {}. URL=[{}]. Response=[{}].", resultRequest.getRetStatus().name(),
					url, response);
			consumerAPI.updateServiceCallResult(resultRequest);
		}
	}

	private ServiceCallResult createServiceCallResult(URI uri) {
		ServiceCallResult resultRequest = new ServiceCallResult();
		String serviceName = uri.getHost();
		resultRequest.setService(serviceName);
		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetStatus(RetStatus.RetSuccess);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		return resultRequest;
	}
}
