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

package com.tencent.cloud.tsf.demo.consumer;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.tsf.annotation.EnableTsf;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableFeignClients // 使用Feign微服务调用时请启用
@EnableTsf
public class ConsumerApplication {
	public static void main(String[] args) {
		SpringApplication.run(ConsumerApplication.class, args);
	}

	@Bean
	@RefreshScope
	@ConditionalOnProperty(value = "server.ssl.bundle", havingValue = "tsf")
	public HttpClientConnectionManager connectionManagerWithSSL(SslBundles sslBundles) {
		SSLContext sslContext = sslBundles.getBundle("tsf").createSslContext();
		SSLContext.setDefault(sslContext);
		return PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(new DefaultClientTlsStrategy(
						sslContext,
						HostnameVerificationPolicy.CLIENT,
						NoopHostnameVerifier.INSTANCE))
				.build();
	}

	@Bean
	@ConditionalOnExpression("'${server.ssl.bundle:}' != 'tsf'")
	public HttpClientConnectionManager connectionManager() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(null, (chain, authType) -> true)
				.build();
		return PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(new DefaultClientTlsStrategy(
						sslContext,
						HostnameVerificationPolicy.CLIENT,
						NoopHostnameVerifier.INSTANCE))
				.build();
	}

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate(
			RestTemplateBuilder builder, HttpClientConnectionManager connectionManager) {
		return builder
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(
						HttpClients.custom()
								.setConnectionManager(connectionManager)
								.build()))
				.build();
	}
}
