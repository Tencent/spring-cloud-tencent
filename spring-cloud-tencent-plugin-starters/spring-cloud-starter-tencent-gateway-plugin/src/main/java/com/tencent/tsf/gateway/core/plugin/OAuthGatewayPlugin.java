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

package com.tencent.tsf.gateway.core.plugin;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.plugin.gateway.context.Position;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;
import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.constant.HttpMethod;
import com.tencent.tsf.gateway.core.exception.TsfGatewayError;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.http.HttpConnectionPoolUtil;
import com.tencent.tsf.gateway.core.model.OAuthPlugin;
import com.tencent.tsf.gateway.core.model.OAuthResult;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import static com.tencent.tsf.gateway.core.constant.GatewayConstant.GATEWAY_WILDCARD_SERVICE_NAME;

public class OAuthGatewayPlugin implements IGatewayPlugin<OAuthPlugin> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final LoadBalancerClientFactory clientFactory;
	@Value("${tsf.gateway.plugin.oauth.authorization:true}")
	Boolean openAuthorization;
	private final PolarisSDKContextManager polarisSDKContextManager;

	public OAuthGatewayPlugin(LoadBalancerClientFactory clientFactory, PolarisSDKContextManager polarisSDKContextManager) {
		this.clientFactory = clientFactory;
		this.polarisSDKContextManager = polarisSDKContextManager;
	}

	@Override
	public PluginPayload startUp(OAuthPlugin pluginInfo, TsfGatewayRequest tsfGatewayRequest) {
		//OAuth认证逻辑
		//构建http请求，访问第三方认证服务器
		Map<String, String[]> parameterMap = tsfGatewayRequest.getParameterMap();
		Map<String, String> paramsMap = new HashMap<>();
		Map<String, String> headerParamsMap = new HashMap<>();
		String tokenAuthUrl = pluginInfo.getTokenAuthUrl();
		//认证结果
		String authResult = null;
		//获取token携带位置
		String tokenBaggagePosition = pluginInfo.getTokenBaggagePosition();
		if (Position.fromString(tokenBaggagePosition) == null) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "tokenBaggagePosition is wrong");
		}
		if (Position.HEADER.equals(Position.fromString(tokenBaggagePosition))) {
			logger.info("header is : {} = {} ", pluginInfo.getTokenKeyName(), tsfGatewayRequest.getRequestHeader(
					pluginInfo.getTokenKeyName()));
			if (StringUtils.isEmpty(tsfGatewayRequest.getRequestHeader(pluginInfo.getTokenKeyName()))) {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "header token value is empty");
			}
			headerParamsMap.put(pluginInfo.getTokenKeyName(), tsfGatewayRequest.getRequestHeader(pluginInfo.getTokenKeyName()));
		}
		else {
			//queryParam中取
			if (parameterMap.get(pluginInfo.getTokenKeyName()) == null || parameterMap.get(pluginInfo.getTokenKeyName()).length == 0) {
				logger.info("parameterMap is empty");
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "parameter token value is empty");
			}
			paramsMap.put(pluginInfo.getTokenKeyName(), parameterMap.get(pluginInfo.getTokenKeyName())[0]);
		}

		// 添加Oauth Authorization授权
		String headerAuthorization = tsfGatewayRequest.getRequestHeader("Authorization");
		if (openAuthorization && StringUtils.isNotBlank(headerAuthorization)) {
			logger.info("[tsf-gateway] OAuthGatewayPlugin openAuthorization is true, put headerAuthorization:{}", headerAuthorization);
			headerParamsMap.put("authorization", headerAuthorization);
		}

		//构建认证请求方法
		String tokenAuthMethod = pluginInfo.getTokenAuthMethod();
		if (HttpMethod.getHttpMethod(tokenAuthMethod) == null) {
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "tokenAuthMethod is wrong");
		}

		Integer timeout = pluginInfo.getExpireTime() != null ? 1000 * pluginInfo.getExpireTime() : null;

		if (StringUtils.isNotBlank(pluginInfo.getTokenAuthServiceName())) {
			// 认证地址选择的是微服务时
			authResult = tokenAuthByMicroservice(pluginInfo, tsfGatewayRequest, paramsMap, headerParamsMap, tokenAuthUrl, tokenAuthMethod, timeout);
		}
		else {
			// 认证地址选择的是外部地址时
			authResult = sendAuthRequestByHttpMethod(paramsMap, headerParamsMap, tokenAuthUrl, tokenAuthMethod, timeout);
		}

		OAuthResult oAuthResult = null;
		try {
			//反序列化authResult为OAuthResult
			//若反序列化失败，则认为用户的认证服务器未遵守tsf微服务网关OAuth插件的使用协议，导致认证失败。
			oAuthResult = JacksonUtils.deserialize(authResult, OAuthResult.class);
		}
		catch (Throwable t) {
			logger.error("[tsf-gateway] authResult deserialize data to OAuthResult occur exception.", t);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_SERIALIZE_ERROR, "oAuthResult deserialize occur error");
		}

		PluginPayload pluginPayload = new PluginPayload();
		if (!oAuthResult.getResult()) {
			//若认证失败,判断是否跳转到用户重定向页面
			if (StringUtils.isNotEmpty(pluginInfo.getRedirectUrl())) {
				pluginPayload.setRedirectUrl(pluginInfo.getRedirectUrl());
			}
			else {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "Token Auth failed");
			}

		}

		if (StringUtils.isNotBlank(pluginInfo.getPayloadMappingName())) {
			//若成功,则封装payload字段映射到后台服务
			if (Position.fromString(pluginInfo.getPayloadMappingPosition()) == null) {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_FAILED, "payloadMappingPosition is wrong");
			}

			if (Position.HEADER.equals(Position.fromString(pluginInfo.getPayloadMappingPosition()))) {
				Map<String, String> headerMap = new HashMap<>();
				headerMap.put(pluginInfo.getPayloadMappingName(), oAuthResult.getPayload());
				pluginPayload.setRequestHeaders(headerMap);
			}
			else if (Position.QUERY.equals(Position.fromString(pluginInfo.getPayloadMappingPosition()))) {
				//queryParam中取
				Map<String, String[]> queryMap = new HashMap<>();
				queryMap.put(pluginInfo.getPayloadMappingName(), new String[] {oAuthResult.getPayload()});
				pluginPayload.setParameterMap(queryMap);
			}
		}

		return pluginPayload;

	}

	private String tokenAuthByMicroservice(OAuthPlugin pluginInfo, TsfGatewayRequest tsfGatewayRequest,
			Map<String, String> paramsMap, Map<String, String> headerParamsMap,
			String tokenAuthUrl, String tokenAuthMethod, Integer timeout) {

		String serviceName = pluginInfo.getTokenAuthServiceName();
		String namespace = null;
		String backupNamespace = MetadataContextHolder.get().getContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
				MetadataConstant.POLARIS_TARGET_NAMESPACE);
		if (serviceName.contains("/")) {
			String[] parts = serviceName.split("/");
			namespace = parts[0];
			serviceName = parts[1];
		}
		try {
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, namespace);
			ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceName,
					ReactorServiceInstanceLoadBalancer.class);

			Response<ServiceInstance> instance = loadBalancer.choose(new DefaultRequest<>()).toFuture().get();
			URI uri = tsfGatewayRequest.getUri();
			if (logger.isDebugEnabled()) {
				logger.debug("[tokenAuthByMicroservice] plugin:{}, tsfGatewayRequest:{}", pluginInfo, tsfGatewayRequest);
			}
			if (instance.getServer() == null) {
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_REQUEST_NOT_FOUND, "Unable to find instance for " + pluginInfo.getTokenAuthServiceName());
			}

			Object otScope = fillTracingContext(namespace, serviceName);

			String newRequestUrl = uri.getScheme() + "://" + GATEWAY_WILDCARD_SERVICE_NAME + tokenAuthUrl;
			URI newUri = new URI(newRequestUrl);
			// 这个requestUrl是从注册中心拿到服务列表并且balance之后带有IP信息的URL
			// http://127.0.0.1:8080/group1/namespace1/Consumer-demo/echo-rest/1?user=1
			URI requestUrl = this.reconstructURI(new OauthDelegatingServiceInstance(instance.getServer()), newUri);
			logger.debug("LoadBalancerClientFilter url chosen: {}", requestUrl);
			String result = sendAuthRequestByHttpMethod(paramsMap, headerParamsMap, requestUrl.toASCIIString(), tokenAuthMethod, timeout);

			if (ClassUtils.isClassPresent("io.opentelemetry.context.Scope") && otScope instanceof Scope) {
				((Scope) otScope).close();
			}
			return result;
		}
		catch (Exception e) {
			logger.error("MicroService {} Request Auth Server Error", tokenAuthMethod, e);
			throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "MicroService {} Request Auth Server Error", tokenAuthMethod);
		}
		finally {
			// restore context
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_APPLICATION_NONE,
					MetadataConstant.POLARIS_TARGET_NAMESPACE, backupNamespace);
		}
	}

	protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	private String sendAuthRequestByHttpMethod(Map<String, String> paramsMap, Map<String, String> headerParamsMap,
			String tokenAuthUrl, String tokenAuthMethod, Integer timeout) {
		if (HttpMethod.GET.equals(HttpMethod.getHttpMethod(tokenAuthMethod))) {
			try {
				return HttpConnectionPoolUtil.httpGet(tokenAuthUrl, paramsMap, headerParamsMap, timeout);
			}
			catch (Throwable e) {
				logger.error("GET Request Auth Server Error", e);
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "DirectAddress GET Request Auth Server Error");
			}
		}
		else {
			//POST请求
			try {
				return HttpConnectionPoolUtil.httpPostWithJSON(tokenAuthUrl, paramsMap, null, headerParamsMap, timeout);
			}
			catch (Throwable e) {
				logger.error("GET Request Auth Server Error", e);
				throw new TsfGatewayException(TsfGatewayError.GATEWAY_AUTH_ERROR, "DirectAddress POST Request Auth Server Error");
			}
		}
	}

	Object fillTracingContext(String namespace, String serviceName) {

		Map<String, String> attributes = new HashMap<>();
		attributes.put("net.peer.service", serviceName);
		attributes.put("remote.namespace-id", namespace);

		TraceAttributes traceAttributes = new TraceAttributes();
		traceAttributes.setAttributes(attributes);
		traceAttributes.setAttributeLocation(TraceAttributes.AttributeLocation.BAGGAGE);

		AssemblyAPI assemblyAPI = polarisSDKContextManager.getAssemblyAPI();
		assemblyAPI.updateTraceAttributes(traceAttributes);

		return traceAttributes.getOtScope();
	}

	class OauthDelegatingServiceInstance implements ServiceInstance {
		final ServiceInstance delegate;

		OauthDelegatingServiceInstance(ServiceInstance delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getServiceId() {
			return delegate.getServiceId();
		}

		@Override
		public String getHost() {
			return delegate.getHost();
		}

		@Override
		public int getPort() {
			return delegate.getPort();
		}

		@Override
		public boolean isSecure() {
			return delegate.isSecure();
		}

		@Override
		public URI getUri() {
			return delegate.getUri();
		}

		@Override
		public Map<String, String> getMetadata() {
			return delegate.getMetadata();
		}

		@Override
		public String getScheme() {
			return delegate.getScheme();
		}

	}
}
