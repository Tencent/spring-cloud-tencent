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

package com.tencent.cloud.plugin.fault;

import java.util.HashMap;

import com.tencent.cloud.common.constant.ContextConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.plugin.fault.config.FaultInjectionProperties;
import com.tencent.cloud.plugin.fault.instrument.resttemplate.PolarisFaultInjectionHttpResponse;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPlugin;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginType;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedRequestContext;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.fault.api.core.FaultAPI;
import com.tencent.polaris.fault.api.rpc.AbortResult;
import com.tencent.polaris.fault.api.rpc.DelayResult;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;
import com.tencent.polaris.fault.client.exception.FaultInjectionException;
import com.tencent.polaris.metadata.core.MetadataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE;
import static com.tencent.cloud.rpc.enhancement.plugin.PluginOrderConstant.ClientPluginOrder.FAULT_INJECTION_PLUGIN_ORDER;

/**
 * Fault injection pre plugin.
 *
 * @author Haotian Zhang
 */
public class FaultInjectionPrePlugin implements EnhancedPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(FaultInjectionPrePlugin.class);

	private final FaultAPI faultAPI;

	private final FaultInjectionProperties faultInjectionProperties;

	public FaultInjectionPrePlugin(FaultAPI faultAPI, FaultInjectionProperties faultInjectionProperties) {
		this.faultAPI = faultAPI;
		this.faultInjectionProperties = faultInjectionProperties;
	}

	@Override
	public String getName() {
		return FaultInjectionPrePlugin.class.getName();
	}

	@Override
	public EnhancedPluginType getType() {
		return EnhancedPluginType.Client.PRE;
	}

	@Override
	public void run(EnhancedPluginContext context) throws Throwable {
		if (!faultInjectionProperties.isEnabled()) {
			return;
		}

		EnhancedRequestContext request = context.getRequest();
		String governanceNamespace = StringUtils.isNotEmpty(request.getGovernanceNamespace()) ? request.getGovernanceNamespace() : MetadataContext.LOCAL_NAMESPACE;
		FaultRequest faultRequest = new FaultRequest(LOCAL_NAMESPACE, LOCAL_SERVICE, governanceNamespace, request.getHost(), MetadataContextHolder.get());

		FaultResponse faultResponse = faultAPI.fault(faultRequest);
		if (faultResponse.isFaultInjected()) {
			if (faultResponse.getDelayResult() != null && faultResponse.getDelayResult().getDelay() > 0) {
				DelayResult delayResult = faultResponse.getDelayResult();
				Thread.sleep(delayResult.getDelay());
			}
			if (faultResponse.getAbortResult() != null) {
				AbortResult abortResult = faultResponse.getAbortResult();
				CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(abortResult.getAbortCode(), new HashMap<>(), "");
				if (ClassUtils.isClassPresent("org.springframework.http.client.ClientHttpResponse")) {
					Object fallbackResponse = new PolarisFaultInjectionHttpResponse(fallbackInfo);
					putMetadataObjectValue(ContextConstant.FaultInjection.FAULT_INJECTION_FALLBACK_HTTP_RESPONSE, fallbackResponse);
				}
				throw new FaultInjectionException(fallbackInfo);
			}
		}
	}

	private void putMetadataObjectValue(String key, Object value) {
		MetadataContextHolder.get().getMetadataContainer(MetadataType.APPLICATION, true).
				putMetadataObjectValue(key, value);
	}

	@Override
	public void handlerThrowable(EnhancedPluginContext context, Throwable throwable) {
		LOG.error("FaultInjectionPrePlugin runs failed. context=[{}].", context, throwable);
	}

	@Override
	public int getOrder() {
		return FAULT_INJECTION_PLUGIN_ORDER;
	}
}
