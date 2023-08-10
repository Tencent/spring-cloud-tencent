/*
 * Copyright (c) 2023 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
	// 此类给一些客户定制化逻辑做一些特殊业务分组文件的配置处理
	private static PolarisConfigCustomExtensionLayer polarisConfigCustomExtensionLayer;
	static {
		ServiceLoader<PolarisConfigCustomExtensionLayer> polarisConfigCustomExtensionLayerLoader = ServiceLoader.load(PolarisConfigCustomExtensionLayer.class);
		Iterator<PolarisConfigCustomExtensionLayer> polarisConfigCustomExtensionLayerIterator = polarisConfigCustomExtensionLayerLoader.iterator();
		// 一般就一个实现类，如果有多个，那么加载的是最后一个
		while (polarisConfigCustomExtensionLayerIterator.hasNext()) {
			polarisConfigCustomExtensionLayer = polarisConfigCustomExtensionLayerIterator.next();
			LOGGER.info("[SCT Config] PolarisConfigFileLocator init polarisConfigCustomExtensionLayer:{}", polarisConfigCustomExtensionLayer);
		}
	}

	public static PolarisConfigCustomExtensionLayer getPolarisConfigCustomExtensionLayer() {
		return polarisConfigCustomExtensionLayer;
	}
}
