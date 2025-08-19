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

package com.tencent.cloud.tsf.msgw.scg;

import java.time.Duration;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.tsf.annotation.EnableTsf;

/**
 * @author seanlxliu
 */
@SpringBootApplication
@EnableTsf
public class ScgApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScgApplication.class, args);
	}

	@Bean
	@RefreshScope
	@ConditionalOnProperty(value = "server.ssl.bundle", havingValue = "tsf")
	public HttpClient httpClient(SslBundles sslBundles) throws SSLException {
		// 自定义连接池
		ConnectionProvider connectionProvider = ConnectionProvider.builder("customPool")
				.maxConnections(500)
				.pendingAcquireTimeout(Duration.ofSeconds(10))
				.build();

		// 配置 SSL 上下文
		SslBundle sslBundle = sslBundles.getBundle("tsf");

		SslContext sslContext = SslContextBuilder.forClient()
				.keyManager(sslBundle.getManagers().getKeyManagerFactory())
				.trustManager(sslBundle.getManagers().getTrustManagerFactory())
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();

		// 自定义 HttpClient
		return HttpClient.create(connectionProvider)
				.responseTimeout(Duration.ofSeconds(5))
				.secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
	}
}
