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

package com.tencent.cloud.polaris.config.tsf.controller;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.polaris.config.adapter.PolarisPropertySource;
import com.tencent.cloud.polaris.config.adapter.PolarisPropertySourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author juanyinyang
 * @Date 2023年8月2日 下午5:08:29
 */
@RestController
public class PolarisAdaptorTsfConfigController {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisAdaptorTsfConfigController.class);

	@Autowired
	Environment environment;

	public PolarisAdaptorTsfConfigController() {
		LOG.info("init PolarisAdaptorTsfConfigController");
	}

	/**
	 * 兼容目前TSF控制台的用法，提供北极星查询当前SDK配置接口.
	 */
	@RequestMapping("/tsf/innerApi/config/findAllConfig")
	public Map<String, Object> findAllConfig() {
		List<PolarisPropertySource> propertySourceList = PolarisPropertySourceManager.getAllPropertySources();

		Set<String> keys = new HashSet<>();
		for (PolarisPropertySource propertySource : propertySourceList) {
			keys.addAll(Arrays.asList(propertySource.getPropertyNames()));
		}

		return keys.stream()
				.collect(HashMap::new, (map, key) -> map.put(key, environment.getProperty(key)), HashMap::putAll);
	}

}
