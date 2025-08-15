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

package com.tencent.cloud.plugin.tsf.tls.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.tencent.polaris.api.config.ConfigProvider;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.plugin.certificate.CertFile;
import com.tencent.polaris.api.plugin.certificate.CertFileKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.certificate.api.core.CertificateAPI;
import com.tencent.polaris.certificate.factory.CertificateAPIFactory;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.certificate.tsf.TsfCertificateManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for sync.
 *
 * @author Haotian Zhang
 */
public final class SyncUtils {

	private static final Logger log = LoggerFactory.getLogger(SyncUtils.class);
	private static final Map<CertFileKey, CertFile> pemFileMap = new HashMap<>();
	private volatile static SDKContext certificateSDKContext;
	private volatile static CertificateAPI certificateAPI;

	private SyncUtils() {

	}

	public static void init(String commonName, String address, String certPath, String token, Long validityDuration,
			Long refreshBefore, Long watchInterval) {
		log.info("begin init SyncUtils with commonName: {}, address: {}, certPath: {}, token: {}", commonName, address, certPath, token);
		try {
			if (!isInitialized() && StringUtils.isNotBlank(commonName) && StringUtils.isNotBlank(address)) {
				initCertificateSDKContext(commonName, address, certPath, token, validityDuration, refreshBefore, watchInterval);
				log.info("init SyncUtils with commonName: {}, address: {}, certPath: {}, token: {} successfully", commonName, address, certPath, token);
			}
		}
		catch (Throwable throwable) {
			log.error("init SyncUtils with commonName: {}, address: {}, certPath: {}, token: {} failed.", commonName, address, certPath, token, throwable);
		}
	}

	private static void initCertificateSDKContext(String commonName, String address, String certPath, String token,
			Long validityDuration, Long refreshBefore, Long watchInterval) {
		// 1. Read user-defined polaris.yml configuration
		ConfigurationImpl configuration = (ConfigurationImpl) ConfigAPIFactory
				.defaultConfig(ConfigProvider.DEFAULT_CONFIG);

		// 2. Override user-defined polaris.yml configuration with SCT configuration
		configuration.getGlobal().getAPI().setReportEnable(false);
		configuration.getGlobal().getStatReporter().setEnable(false);
		configuration.getConsumer().getOutlierDetection().setWhen(OutlierDetectionConfig.When.never);
		configuration.getGlobal().getCertificate().setEnable(true);
		configuration.getGlobal().getCertificate().setCommonName(commonName);
		configuration.getGlobal().getCertificate().setPluginName(DefaultPlugins.TSF_CERTIFICATE_MANAGER);
		if (validityDuration != null) {
			configuration.getGlobal().getCertificate().setValidityDuration(validityDuration);
		}
		if (refreshBefore != null) {
			configuration.getGlobal().getCertificate().setRefreshBefore(refreshBefore);
		}
		if (watchInterval != null) {
			configuration.getGlobal().getCertificate().setWatchInterval(watchInterval);
		}
		TsfCertificateManagerConfig tsfCertificateManagerConfig = new TsfCertificateManagerConfig();
		tsfCertificateManagerConfig.setAddress(address);
		tsfCertificateManagerConfig.setCertPath(certPath);
		tsfCertificateManagerConfig.setToken(token);
		configuration.getGlobal().getCertificate()
				.setPluginConfig(DefaultPlugins.TSF_CERTIFICATE_MANAGER, tsfCertificateManagerConfig);

		certificateSDKContext = SDKContext.initContextByConfig(configuration);
		certificateSDKContext.init();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (Objects.nonNull(certificateSDKContext)) {
					certificateSDKContext.destroy();
					certificateSDKContext = null;
				}
				log.info("Polaris SDK certificate context is destroyed.");
			}
			catch (Throwable throwable) {
				log.info("Polaris SDK certificate context is destroyed failed.", throwable);
			}
		}));
		log.info("create Polaris certificate SDK context successfully.");

		certificateAPI = CertificateAPIFactory.createCertificateAPIByContext(certificateSDKContext);
		pemFileMap.putAll(certificateAPI.getPemFileMap());
	}

	public static boolean isInitialized() {
		return certificateSDKContext != null && certificateAPI != null;
	}

	public static boolean isVerified() {
		return isInitialized() && CollectionUtils.isNotEmpty(certificateAPI.getPemFileMap());
	}

	public static String getPemKeyStoreCertPath() {
		if (pemFileMap.containsKey(CertFileKey.PemKeyStoreCertPath)) {
			return pemFileMap.get(CertFileKey.PemKeyStoreCertPath).getPath();
		}
		return null;
	}

	public static String getPemKeyStoreKeyPath() {
		if (pemFileMap.containsKey(CertFileKey.PrivateKeyFile)) {
			return pemFileMap.get(CertFileKey.PrivateKeyFile).getPath();
		}
		return null;
	}

	public static String getPemTrustStoreCertPath() {
		if (pemFileMap.containsKey(CertFileKey.PemTrustStoreCertPath)) {
			return pemFileMap.get(CertFileKey.PemTrustStoreCertPath).getPath();
		}
		return null;
	}
}
