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

package com.tencent.cloud.plugin.unit.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tencent.cloud.common.tsf.ConditionalOnOnlyTsfConsulEnabled;
import com.tencent.cloud.plugin.unit.instrument.feign.UnitFeignRequestInterceptor;
import com.tencent.cloud.plugin.unit.instrument.resttemplate.UnitRestTemplateInterceptor;
import com.tencent.cloud.plugin.unit.plugin.UnitClientFinallyEnhancedPlugin;
import com.tencent.cloud.plugin.unit.plugin.UnitFeignEnhancedPlugin;
import com.tencent.cloud.plugin.unit.plugin.UnitRestTemplateEnhancedPlugin;
import com.tencent.cloud.plugin.unit.plugin.UnitServletPreEnhancedPlugin;
import com.tencent.tsf.unit.aspect.TsfUnitRouteAspect;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.TsfZoneFilterUnitCallback;
import com.tencent.tsf.unit.core.utils.TencentUnitUtils;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;


@Configuration(proxyBeanMethods = false)
@ConditionalOnOnlyTsfConsulEnabled
public class UnitAutoConfiguration {

	@Bean
	public UnitBeanPostProcessor unitPolarisDiscoveryClientBeanPostProcessor() {
		return new UnitBeanPostProcessor();
	}

	@Bean
	public UnitClientFinallyEnhancedPlugin unitClientFinallyEnhancedPlugin() {
		return new UnitClientFinallyEnhancedPlugin();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static class UnitServletFilterConfig {

		@Bean
		public UnitServletPreEnhancedPlugin unitServletPreEnhancedPlugin() {
			return new UnitServletPreEnhancedPlugin();
		}
	}

	/**
	 * 如果不是网关才启动，避免重复调用.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.cloud.gateway.filter.GlobalFilter")
	static class MicroserviceUnitEnable {
		static {
			TencentUnitManager.addRuleCallback(new TsfZoneFilterUnitCallback());
			TencentUnitUtils.enable();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
	static class TsfUnitRestTemplateConfig {

		@Autowired(required = false)
		private List<RestTemplate> restTemplates = Collections.emptyList();


		@Bean
		public UnitRestTemplateInterceptor tsfUnitRestTemplateInterceptor() {
			return new UnitRestTemplateInterceptor();
		}

		@Bean
		public SmartInitializingSingleton addTsfUnitRestTemplateInterceptorForRestTemplate(UnitRestTemplateInterceptor interceptor) {
			return () -> restTemplates.forEach(restTemplate -> {
				List<ClientHttpRequestInterceptor> list = new ArrayList<>(restTemplate.getInterceptors());
				list.add(interceptor);
				restTemplate.setInterceptors(list);
			});
		}

		@Bean
		public UnitRestTemplateEnhancedPlugin unitRestTemplateEnhancedPlugin() {
			return new UnitRestTemplateEnhancedPlugin();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "feign.Feign")
	static class TsfUnitFeignConfig {

		@Bean
		@ConditionalOnMissingBean
		public UnitFeignRequestInterceptor tsfUnitFeignRequestInterceptor() {
			return new UnitFeignRequestInterceptor();
		}

		@Bean
		public TsfUnitRouteAspect tsfUnitRouteAspect() {
			return new TsfUnitRouteAspect();
		}

		@Bean
		public UnitFeignEnhancedPlugin unitFeignEnhancedPlugin() {
			return new UnitFeignEnhancedPlugin();
		}
	}
}
