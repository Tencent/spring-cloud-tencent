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

package com.tencent.cloud.plugin.trace.config;

import java.util.List;

import com.tencent.cloud.plugin.trace.TraceClientFinallyEnhancedPlugin;
import com.tencent.cloud.plugin.trace.TraceClientPreEnhancedPlugin;
import com.tencent.cloud.plugin.trace.TraceServerFinallyEnhancedPlugin;
import com.tencent.cloud.plugin.trace.TraceServerPreEnhancedPlugin;
import com.tencent.cloud.plugin.trace.attribute.PolarisSpanAttributesProvider;
import com.tencent.cloud.plugin.trace.attribute.SpanAttributesProvider;
import com.tencent.cloud.plugin.trace.attribute.tsf.TsfSpanAttributesProvider;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@ConditionalOnProperty(value = "spring.cloud.polaris.trace.enabled", matchIfMissing = true)
public class TraceEnhancedPluginAutoConfiguration {

	@Bean
	public TraceServerPreEnhancedPlugin traceServerPreEnhancedPlugin(
			PolarisSDKContextManager polarisSDKContextManager, @Autowired(required = false) List<SpanAttributesProvider> spanAttributesProviderList) {
		return new TraceServerPreEnhancedPlugin(polarisSDKContextManager, spanAttributesProviderList);
	}

	@Bean
	public TraceServerFinallyEnhancedPlugin traceServerFinallyEnhancedPlugin(
			PolarisSDKContextManager polarisSDKContextManager, @Autowired(required = false) List<SpanAttributesProvider> spanAttributesProviderList) {
		return new TraceServerFinallyEnhancedPlugin(polarisSDKContextManager, spanAttributesProviderList);
	}

	@Bean
	public TraceClientPreEnhancedPlugin traceClientPreEnhancedPlugin(
			PolarisSDKContextManager polarisSDKContextManager, @Autowired(required = false) List<SpanAttributesProvider> spanAttributesProviderList) {
		return new TraceClientPreEnhancedPlugin(polarisSDKContextManager, spanAttributesProviderList);
	}

	@Bean
	public TraceClientFinallyEnhancedPlugin traceClientFinallyEnhancedPlugin() {
		return new TraceClientFinallyEnhancedPlugin();
	}

	@Bean
	@ConditionalOnMissingBean
	public PolarisSpanAttributesProvider polarisSpanAttributesProvider() {
		return new PolarisSpanAttributesProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public TsfSpanAttributesProvider tsfClientSpanAttributesProvider() {
		return new TsfSpanAttributesProvider();
	}
}
