/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
