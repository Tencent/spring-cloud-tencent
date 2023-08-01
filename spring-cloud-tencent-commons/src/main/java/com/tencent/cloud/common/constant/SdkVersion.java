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

package com.tencent.cloud.common.constant;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constant for SDK version.
 *
 * @author Haotian Zhang
 */
public final class SdkVersion {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * SDK version key.
	 * When packaging through the Maven plug-in, the version information of the Core package is written into META-INF,
	 * and the SDK version is registered in the registration center when the service is registered.
	 * Use with maven-jar-plugin in the pom.xml file
	 */
	public static String SDK_VERSION_KEY = "SCT_SDK_VERSION";
	private static String version;

	private SdkVersion() {
	}

	public static String get() {
		if (version != null) {
			LOG.info("SDK SDK Version: {}", version);
			return version;
		}
		try {
			Enumeration<URL> resources = SdkVersion.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				Manifest manifest = new Manifest(resources.nextElement().openStream());
				Attributes attrs = manifest.getMainAttributes();
				if (attrs == null) {
					continue;
				}
				String name = attrs.getValue("Bundle-Name");
				if (SDK_VERSION_KEY.equals(name)) {
					String version = attrs.getValue("Bundle-Version");
					LOG.info("SCT SDK Version: {}", version);
					SdkVersion.version = version;
					break;
				}
			}
			return version;
		}
		catch (Exception exception) {
			// ignore it
		}
		LOG.info("could not get bundle : '{}' version, please check MANIFEST.MF format." +
				" return default version: UNKNOWN", SDK_VERSION_KEY);
		return "UNKNOWN";
	}

}
