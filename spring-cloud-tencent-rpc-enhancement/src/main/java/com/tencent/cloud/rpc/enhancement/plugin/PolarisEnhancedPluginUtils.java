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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.RequestLabelUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BANDWIDTH_LIMIT_EXCEEDED;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.INSUFFICIENT_STORAGE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.LOOP_DETECTED;
import static org.springframework.http.HttpStatus.NETWORK_AUTHENTICATION_REQUIRED;
import static org.springframework.http.HttpStatus.NOT_EXTENDED;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.VARIANT_ALSO_NEGOTIATES;

/**
 * Abstract Polaris Plugin Adapter .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022-07-11
 */
public final class PolarisEnhancedPluginUtils {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisEnhancedPluginUtils.class);
	private static final List<HttpStatus> HTTP_STATUSES = toList(NOT_IMPLEMENTED, BAD_GATEWAY,
			SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, HTTP_VERSION_NOT_SUPPORTED, VARIANT_ALSO_NEGOTIATES,
			INSUFFICIENT_STORAGE, LOOP_DETECTED, BANDWIDTH_LIMIT_EXCEEDED, NOT_EXTENDED, NETWORK_AUTHENTICATION_REQUIRED);

	private PolarisEnhancedPluginUtils() {

	}

	/**
	 * createServiceCallResult.
	 * @param callerHost caller host ip
	 * @param calleeServiceName will pick up url host when null
	 * @param calleeHost will pick up url host when null
	 * @param calleePort will pick up url port when null
	 * @param uri request url
	 * @param requestHeaders request header
	 * @param responseHeaders response header
	 * @param statusCode response status
	 * @param delay delay
	 * @param exception exception
	 * @return ServiceCallResult
	 */
	public static ServiceCallResult createServiceCallResult(String callerHost,
			@Nullable String calleeServiceName, @Nullable String calleeHost, @Nullable Integer calleePort,
			URI uri, HttpHeaders requestHeaders, @Nullable HttpHeaders responseHeaders,
			@Nullable Integer statusCode, long delay, @Nullable Throwable exception) {

		ServiceCallResult resultRequest = new ServiceCallResult();
		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		resultRequest.setService(StringUtils.isBlank(calleeServiceName) ? uri.getHost() : calleeServiceName);
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetCode(statusCode == null ? -1 : statusCode);
		resultRequest.setDelay(delay);
		resultRequest.setCallerService(new ServiceKey(MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE));
		resultRequest.setCallerIp(callerHost);
		resultRequest.setHost(StringUtils.isBlank(calleeHost) ? uri.getHost() : calleeHost);
		resultRequest.setPort(calleePort == null ? getPort(uri) : calleePort);
		resultRequest.setLabels(getLabels(requestHeaders));
		resultRequest.setRetStatus(getRetStatusFromRequest(responseHeaders, statusCode, exception));
		resultRequest.setRuleName(getActiveRuleNameFromRequest(responseHeaders));
		return resultRequest;
	}

	/**
	 * createInstanceResourceStat.
	 * @param calleeServiceName will pick up url host when null
	 * @param calleeHost will pick up url host when null
	 * @param calleePort will pick up url port when null
	 * @param uri request url
	 * @param statusCode response status
	 * @param delay delay
	 * @param exception exception
	 * @return ResourceStat
	 */
	public static ResourceStat createInstanceResourceStat(
			@Nullable String calleeServiceName, @Nullable String calleeHost, @Nullable Integer calleePort,
			URI uri, @Nullable Integer statusCode, long delay, @Nullable Throwable exception) {
		ServiceKey calleeServiceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, StringUtils.isBlank(calleeServiceName) ? uri.getHost() : calleeServiceName);
		ServiceKey callerServiceKey = new ServiceKey(MetadataContext.LOCAL_NAMESPACE, MetadataContext.LOCAL_SERVICE);
		Resource resource = new InstanceResource(
				calleeServiceKey,
				StringUtils.isBlank(calleeHost) ? uri.getHost() : calleeHost,
				calleePort == null ? getPort(uri) : calleePort,
				callerServiceKey
		);
		return new ResourceStat(resource, statusCode == null ? -1 : statusCode, delay, getDefaultRetStatus(statusCode, exception));
	}


	/**
	 * Convert items to List.
	 *
	 * @param items item arrays
	 * @param <T>   Object Generics.
	 * @return list
	 */
	@SafeVarargs
	private static <T> List<T> toList(T... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	/**
	 * Callback after completion of request processing, Check if business meltdown reporting is required.
	 *
	 * @param httpStatus request http status code
	 * @return true , otherwise return false .
	 */
	static boolean apply(@Nullable HttpStatus httpStatus) {
		if (Objects.isNull(httpStatus)) {
			return false;
		}
		RpcEnhancementReporterProperties reportProperties;
		try {
			reportProperties = ApplicationContextAwareUtils.getApplicationContext()
					.getBean(RpcEnhancementReporterProperties.class);
		}
		catch (BeansException e) {
			LOG.error("get RpcEnhancementReporterProperties bean err", e);
			reportProperties = new RpcEnhancementReporterProperties();
		}
		// statuses > series
		List<HttpStatus> status = reportProperties.getStatuses();
		if (status.isEmpty()) {
			List<HttpStatus.Series> series = reportProperties.getSeries();
			// Check INTERNAL_SERVER_ERROR (500) status.
			if (reportProperties.isIgnoreInternalServerError() && Objects.equals(httpStatus, INTERNAL_SERVER_ERROR)) {
				return false;
			}
			if (series.isEmpty()) {
				return HTTP_STATUSES.contains(httpStatus);
			}
			return series.contains(httpStatus.series());
		}
		// Use the user-specified fuse status code.
		return status.contains(httpStatus);
	}

	public static RetStatus getRetStatusFromRequest(HttpHeaders headers, Integer statusCode, Throwable exception) {
		return getRetStatusFromRequest(headers, getDefaultRetStatus(statusCode, exception));
	}

	static RetStatus getRetStatusFromRequest(HttpHeaders headers, RetStatus defaultVal) {
		if (headers != null && headers.containsKey(HeaderConstant.INTERNAL_CALLEE_RET_STATUS)) {
			List<String> values = headers.get(HeaderConstant.INTERNAL_CALLEE_RET_STATUS);
			if (CollectionUtils.isNotEmpty(values)) {
				String retStatusVal = com.tencent.polaris.api.utils.StringUtils.defaultString(values.get(0));
				if (Objects.equals(retStatusVal, RetStatus.RetFlowControl.getDesc())) {
					return RetStatus.RetFlowControl;
				}
				if (Objects.equals(retStatusVal, RetStatus.RetReject.getDesc())) {
					return RetStatus.RetReject;
				}
			}
		}
		return defaultVal;
	}

	static String getActiveRuleNameFromRequest(HttpHeaders headers) {
		if (headers != null && headers.containsKey(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME)) {
			Collection<String> values = headers.get(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME);
			if (CollectionUtils.isNotEmpty(values)) {
				String decodedActiveRuleName = "";
				try {
					decodedActiveRuleName = URLDecoder.decode(new ArrayList<>(values).get(0), UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					LOG.error("Cannot decode {} from header internal-callee-activerule.",
							headers.get(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME).get(0), e);
				}
				return com.tencent.polaris.api.utils.StringUtils.defaultString(decodedActiveRuleName);
			}
		}
		return "";
	}

	private static RetStatus getDefaultRetStatus(Integer statusCode, Throwable exception) {
		RetStatus retStatus = RetStatus.RetSuccess;
		if (exception != null) {
			retStatus = RetStatus.RetFail;
			if (exception instanceof SocketTimeoutException) {
				retStatus = RetStatus.RetTimeout;
			}
		}
		else if (statusCode == null || apply(HttpStatus.resolve(statusCode))) {
			retStatus = RetStatus.RetFail;
		}
		return retStatus;
	}

	private static int getPort(URI uri) {
		// -1 means access directly by url, and use http default port number 80
		return uri.getPort() == -1 ? 80 : uri.getPort();
	}

	private static String getLabels(HttpHeaders headers) {
		if (headers != null) {
			Collection<String> labels = headers.get(RouterConstant.ROUTER_LABEL_HEADER);
			if (CollectionUtils.isNotEmpty(labels) && labels.iterator().hasNext()) {
				String label = labels.iterator().next();
				try {
					label = URLDecoder.decode(label, UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					LOG.error("unsupported charset exception " + UTF_8, e);
				}
				return RequestLabelUtils.convertLabel(label);
			}
		}
		return null;
	}

	public static Map<String, String> getLabelMap(HttpHeaders headers) {
		if (headers != null) {
			Collection<String> labels = headers.get(RouterConstant.ROUTER_LABEL_HEADER);
			if (CollectionUtils.isNotEmpty(labels) && labels.iterator().hasNext()) {
				String label = labels.iterator().next();
				try {
					label = URLDecoder.decode(label, UTF_8);
				}
				catch (UnsupportedEncodingException e) {
					LOG.error("unsupported charset exception " + UTF_8, e);
				}
				try {
					return new ObjectMapper().readValue(label, new TypeReference<HashMap<String, String>>() { });
				}
				catch (JsonProcessingException e) {
					LOG.error("parse label map exception", e);
				}
			}
		}
		return null;
	}

}
