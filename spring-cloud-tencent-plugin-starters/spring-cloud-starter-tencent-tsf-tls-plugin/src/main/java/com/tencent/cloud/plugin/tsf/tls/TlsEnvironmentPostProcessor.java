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

package com.tencent.cloud.plugin.tsf.tls;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.plugin.tsf.tls.utils.SyncUtils;
import com.tencent.cloud.polaris.context.config.extend.tsf.TsfTlsProperties;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Environment post processor for polaris tls.
 *
 * @author Haotian Zhang
 */
public class TlsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String address = environment.getProperty("MESH_CITADEL_ADDR");
		if (StringUtils.isNotBlank(address)
				&& StringUtils.equals("tsf", environment.getProperty("server.ssl.bundle"))
				&& ClassUtils.isClassPresent("com.tencent.cloud.plugin.tsf.tls.utils.SyncUtils")
				&& !ClassUtils.isClassPresent("com.tencent.cloud.polaris.config.adapter.PolarisConfigFileLocator")) {
			// get common name
			String commonName = environment.getProperty("spring.cloud.polaris.service");
			if (StringUtils.isBlank(commonName)) {
				commonName = environment.getProperty("spring.cloud.polaris.discovery.service");
			}
			if (StringUtils.isBlank(commonName)) {
				commonName = environment.getProperty("spring.application.name");
			}
			// get certPath
			String certPath = environment.getProperty("MESH_CITADEL_CERT");
			// get token
			String token = environment.getProperty("tsf_token");
			// get validityDuration
			Long validityDuration = environment.getProperty("spring.cloud.polaris.tls.validityDuration", Long.class, TsfTlsProperties.DEFAULT_VALIDITY_DURATION);
			// get refreshBefore
			Long refreshBefore = environment.getProperty("spring.cloud.polaris.tls.refreshBefore", Long.class, TsfTlsProperties.DEFAULT_REFRESH_BEFORE);
			// get watchInterval
			Long watchInterval = environment.getProperty("spring.cloud.polaris.tls.watchInterval", Long.class, TsfTlsProperties.DEFAULT_WATCH_INTERVAL);
			SyncUtils.init(commonName, address, certPath, token, validityDuration, refreshBefore, watchInterval);
			System.setProperty("server.ssl.bundle", "tsf");
			if (SyncUtils.isVerified()) {
				Map<String, Object> tlsEnvProperties = new HashMap<>();
				// set ssl
				String clientAuth = environment.getProperty("server.ssl.client-auth", "want");
				tlsEnvProperties.put("server.ssl.client-auth", clientAuth);
				System.setProperty("server.ssl.client-auth", clientAuth);
				String protocol = environment.getProperty("spring.cloud.polaris.discovery.protocol", "https");
				tlsEnvProperties.put("spring.cloud.polaris.discovery.protocol", protocol);
				System.setProperty("spring.cloud.polaris.discovery.protocol", protocol);
				tlsEnvProperties.put("tsf.discovery.scheme", protocol);
				System.setProperty("tsf.discovery.scheme", protocol);

				// set tsf spring ssl bundle
				tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.reload-on-update", "true");
				if (StringUtils.isNotBlank(SyncUtils.getPemKeyStoreCertPath()) && StringUtils.isNotBlank(SyncUtils.getPemKeyStoreKeyPath())) {
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.keystore.certificate", SyncUtils.getPemKeyStoreCertPath());
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.keystore.private-key", SyncUtils.getPemKeyStoreKeyPath());
				}
				if (StringUtils.isNotBlank(SyncUtils.getPemTrustStoreCertPath())) {
					tlsEnvProperties.put("spring.ssl.bundle.pem.tsf.truststore.certificate", SyncUtils.getPemTrustStoreCertPath());
				}

				// process environment
				MapPropertySource propertySource = new MapPropertySource("tsf-tls-properties", tlsEnvProperties);
				environment.getPropertySources().addFirst(propertySource);
			}
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
