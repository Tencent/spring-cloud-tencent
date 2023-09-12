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
 *
 */
package com.tencent.cloud.polaris.config.adapter;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author juanyinyang
 * @Date 2023年8月10日 下午4:11:05
 */
public final class PolarisServiceLoaderUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolarisServiceLoaderUtil.class);
	private PolarisServiceLoaderUtil() {
	}
	// this class provides customized logic for some customers to configure special business group files
	private static PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer;
	static {
		ServiceLoader<PolarisConfigCustomExtensionLayer> polarisConfigCustomExtensionLayerLoader = ServiceLoader.load(PolarisConfigCustomExtensionLayer.class);
		Iterator<PolarisConfigCustomExtensionLayer> polarisConfigCustomExtensionLayerIterator = polarisConfigCustomExtensionLayerLoader.iterator();
		// Generally, there is only one implementation class. If there are multiple, the last one is loaded
		while (polarisConfigCustomExtensionLayerIterator.hasNext()) {
			polarisConfigCustomExtensionLayer = polarisConfigCustomExtensionLayerIterator.next();
			LOGGER.info("[SCT Config] PolarisConfigFileLocator init polarisConfigCustomExtensionLayer:{}", polarisConfigCustomExtensionLayer);
		}
	}

	public static PolarisConfigCustomExtensionLayer getPolarisConfigCustomExtensionLayer() {
		return polarisConfigCustomExtensionLayer;
	}
}
