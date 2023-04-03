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

package com.tencent.cloud.rpc.enhancement.scg;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.http.HttpStatus;

public class EnhancedPolarisHttpClient extends HttpClient {

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedPolarisHttpClient.class);

	private final RpcEnhancementReporterProperties properties;
	private final SDKContext context;
	private final ConsumerAPI consumerAPI;
	private final Reporter adapter;
	private final BiConsumer<? super HttpClientResponse, ? super Throwable> handler = new BiConsumer<HttpClientResponse, Throwable>() {
		@Override
		public void accept(HttpClientResponse httpClientResponse, Throwable throwable) {
			if (Objects.isNull(consumerAPI)) {
				return;
			}
			HttpHeaders responseHeaders = httpClientResponse.responseHeaders();

			ServiceCallResult result = new ServiceCallResult();
			result.setCallerService(new ServiceKey(MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE));
			result.setNamespace(MetadataContext.LOCAL_NAMESPACE);

			Map<String, String> metadata = MetadataContextHolder.get().getLoadbalancerMetadata();
			result.setDelay(System.currentTimeMillis() - Long.parseLong(metadata.get("startTime")));
			result.setService(metadata.get(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID));
			result.setHost(metadata.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST));
			result.setPort(Integer.parseInt(metadata.get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT)));
			RetStatus status = RetStatus.RetSuccess;
			if (Objects.isNull(throwable)) {
				if (EnhancedPolarisHttpClient.this.adapter.apply(HttpStatus.valueOf(httpClientResponse.status()
						.code()))) {
					status = RetStatus.RetFail;
				}
				org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
				responseHeaders.forEach(entry -> headers.add(entry.getKey(), entry.getValue()));
				status = adapter.getRetStatusFromRequest(headers, status);
				result.setRuleName(adapter.getActiveRuleNameFromRequest(headers));
			}
			else {
				if (throwable instanceof SocketTimeoutException) {
					status = RetStatus.RetTimeout;
				}
			}
			result.setMethod(httpClientResponse.uri());
			result.setRetCode(httpClientResponse.status().code());
			result.setRetStatus(status);
			if (Objects.nonNull(context)) {
				result.setCallerIp(context.getConfig().getGlobal().getAPI().getBindIP());
			}
			try {
				consumerAPI.updateServiceCallResult(result);
			}
			catch (Throwable ex) {
				LOG.error("update service call result fail", ex);
			}
		}
	};
	private HttpClient target;

	public EnhancedPolarisHttpClient(
			HttpClient client,
			RpcEnhancementReporterProperties properties,
			SDKContext context,
			ConsumerAPI consumerAPI) {
		this.properties = properties;
		this.context = context;
		this.consumerAPI = consumerAPI;
		this.target = client;
		this.adapter = new Reporter(properties);
		this.registerReportHandler();
	}

	@Override
	public HttpClientConfig configuration() {
		return target.configuration();
	}

	@Override
	protected HttpClient duplicate() {
		return new EnhancedPolarisHttpClient(target, properties, context, consumerAPI);
	}

	private void registerReportHandler() {
		target = target.doOnRequest((request, connection) -> {
			String serviceId = request.requestHeaders().get(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID);
			String host = request.requestHeaders().get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST);
			String port = request.requestHeaders().get(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT);
			if (StringUtils.isNotBlank(serviceId)) {
				MetadataContextHolder.get().setLoadbalancer(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID, serviceId);
				MetadataContextHolder.get().setLoadbalancer(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST, host);
				MetadataContextHolder.get().setLoadbalancer(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT, port);
				MetadataContextHolder.get().setLoadbalancer("startTime", System.currentTimeMillis() + "");
			}

			request.requestHeaders().remove(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID);
			request.requestHeaders().remove(HeaderConstant.INTERNAL_CALLEE_INSTANCE_HOST);
			request.requestHeaders().remove(HeaderConstant.INTERNAL_CALLEE_INSTANCE_PORT);
		});
		target = target.doOnResponse((httpClientResponse, connection) -> handler.accept(httpClientResponse, null));
		target = target.doOnResponseError(handler);
	}


	private static class Reporter extends AbstractPolarisReporterAdapter {

		/**
		 * Constructor With {@link RpcEnhancementReporterProperties} .
		 *
		 * @param reportProperties instance of {@link RpcEnhancementReporterProperties}.
		 */
		protected Reporter(RpcEnhancementReporterProperties reportProperties) {
			super(reportProperties);
		}

		@Override
		public boolean apply(HttpStatus httpStatus) {
			return super.apply(httpStatus);
		}

		@Override
		public RetStatus getRetStatusFromRequest(org.springframework.http.HttpHeaders headers, RetStatus defaultVal) {
			return super.getRetStatusFromRequest(headers, defaultVal);
		}

		@Override
		public String getActiveRuleNameFromRequest(org.springframework.http.HttpHeaders headers) {
			return super.getActiveRuleNameFromRequest(headers);
		}
	}

}
