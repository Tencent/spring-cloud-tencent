/*
 * Copyright (c) 2023 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 *
 */

package com.tencent.cloud.polaris.config.adapter;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;

import com.tencent.polaris.configuration.api.core.ConfigFileService;


/**
 * @Date Jul 23, 2023 2:57:49 PM
 * @author juanyinyang
 */
public interface PolarisConfigCustomExtensionLayer {
	void execute(String namespace, Environment environment, CompositePropertySource compositePropertySource,
			PolarisPropertySourceManager polarisPropertySourceManager, ConfigFileService configFileService);

}
