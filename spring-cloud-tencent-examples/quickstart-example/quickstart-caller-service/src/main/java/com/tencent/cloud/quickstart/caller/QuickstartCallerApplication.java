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

package com.tencent.cloud.quickstart.caller;

import com.tencent.cloud.polaris.circuitbreaker.resttemplate.PolarisCircuitBreaker;
import com.tencent.cloud.quickstart.caller.circuitbreaker.CustomFallback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Quickstart caller application.
 *
 * @author Haotian Zhang
 */
@SpringBootApplication
@EnableFeignClients
public class QuickstartCallerApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuickstartCallerApplication.class, args);
	}

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	@LoadBalanced
	public RestTemplate defaultRestTemplate() {
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://QuickstartCalleeService");
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		return restTemplate;
	}

	@Bean
	@LoadBalanced
	@PolarisCircuitBreaker
	public RestTemplate restTemplateFallbackFromPolaris() {
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://QuickstartCalleeService");
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		return restTemplate;
	}

	@Bean
	@LoadBalanced
	@PolarisCircuitBreaker(fallbackClass = CustomFallback.class)
	public RestTemplate restTemplateFallbackFromCode() {
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://QuickstartCalleeService");
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		return restTemplate;
	}

	@LoadBalanced
	@Bean
	WebClient.Builder webClientBuilder() {
		return WebClient.builder().baseUrl("http://QuickstartCalleeService");
	}
}
