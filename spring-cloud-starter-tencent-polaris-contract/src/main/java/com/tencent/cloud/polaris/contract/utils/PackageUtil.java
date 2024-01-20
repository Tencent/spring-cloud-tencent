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

package com.tencent.cloud.polaris.contract.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.tencent.cloud.polaris.contract.SwaggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

/**
 * Util for package processing.
 *
 * @author Haotian Zhang
 */
public final class PackageUtil {

	/**
	 * splitter for property.
	 */
	public static final String SPLITTER = ",";
	private static final Logger LOG = LoggerFactory.getLogger(PackageUtil.class);

	private PackageUtil() {
	}

	public static String scanPackage(String configBasePackage) {
		String validScanPackage;
		// Externally configured scan package
		Set<String> configPackageSet = new HashSet<>();
		if (!StringUtils.isEmpty(configBasePackage)) {
			configPackageSet.addAll(Arrays.asList(configBasePackage.split(SPLITTER)));
		}
		Object mainClz = SwaggerContext.getAttribute(String.format("$%s", "MainClass"));
		// Verification of the valid path of MainClass
		if (mainClz != null) {
			Set<String> autoDetectPackageSet = parseDefaultScanPackage((Class<?>) mainClz);
			if (LOG.isInfoEnabled() && !autoDetectPackageSet.isEmpty()) {
				LOG.info("Auto detect default swagger scan packages: {}",
						String.join(SPLITTER, autoDetectPackageSet).trim());
			}
			Set<String> validScanPackageSet = merge(configPackageSet, autoDetectPackageSet);
			validScanPackage = String.join(SPLITTER, validScanPackageSet).trim();
			if (LOG.isInfoEnabled() && !StringUtils.isEmpty(validScanPackage)) {
				LOG.info("Swagger scan valid packages: {}", validScanPackage);
			}
		}
		else {
			// If there is no MainClass, the configured path is used for scanning
			validScanPackage = String.join(SPLITTER, configPackageSet);
			if (LOG.isWarnEnabled()) {
				LOG.warn("Cannot detect main class, swagger scanning packages is set to: {}",
						validScanPackage);
			}
		}
		return validScanPackage;
	}

	public static Set<String> merge(Set<String> configPackageSet, Set<String> autoDetectPackageSet) {
		if (configPackageSet == null || configPackageSet.size() == 0) {
			return autoDetectPackageSet;
		}
		return configPackageSet;
	}


	public static Set<String> parseDefaultScanPackage(Class<?> mainClass) {
		Set<String> packageSets = new HashSet<>();
		String defaultPackage = mainClass.getPackage().getName();
		try {
			boolean springBootEnv = true;
			try {
				Class.forName("org.springframework.boot.autoconfigure.SpringBootApplication");
			}
			catch (Throwable t) {
				LOG.info("Can not load annotation @SpringBootApplication, " +
						"current environment is not in spring boot framework. ");
				springBootEnv = false;
			}
			if (!springBootEnv) {
				packageSets.add(defaultPackage);
				return packageSets;
			}
			SpringBootApplication bootAnnotation = mainClass.getAnnotation(SpringBootApplication.class);
			Class<?>[] baseClassPackages;
			String[] basePackages;
			if (bootAnnotation == null) {
				packageSets.add(defaultPackage);
			}
			else {
				// baseClassPackages annotation
				baseClassPackages = bootAnnotation.scanBasePackageClasses();
				for (Class<?> clz : baseClassPackages) {
					packageSets.add(clz.getPackage().getName());
				}
				// basePackage annotation
				basePackages = bootAnnotation.scanBasePackages();
				packageSets.addAll(Arrays.asList(basePackages));
				// When basePackage and baseClassPackages are both empty, the package path where the MainClass class is located is used by default.
				if (packageSets.isEmpty()) {
					packageSets.add(defaultPackage);
				}
			}
		}
		catch (Throwable t) {
			LOG.warn("Swagger scan package is empty and auto detect main class occur exception: {}",
					t.getMessage());

		}
		return packageSets;
	}
}
