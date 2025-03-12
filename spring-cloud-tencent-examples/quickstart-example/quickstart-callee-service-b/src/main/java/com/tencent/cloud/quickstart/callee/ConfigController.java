/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
package com.tencent.cloud.quickstart.callee;


import com.tencent.cloud.quickstart.callee.config.ConfigurationPropertiesSample;
import com.tencent.cloud.quickstart.callee.config.RefreshScopeSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@RestController
public class ConfigController {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigController.class);

	@Value("${spring.application.name:}")
	private String applicationName;

	@Value("${spring.cloud.client.ip-address:127.0.0.1}")
	private String ip;

	@Autowired
	private ConfigurationPropertiesSample configurationPropertiesSample;

	@Autowired
	private RefreshScopeSample refreshScopeSample;

	@RequestMapping(value = "/config/a", method = RequestMethod.GET)
	public String a() {
		String result = String.format("from application:%s, host-ip: %s, refreshScopeSample: %s",
				applicationName, ip, refreshScopeSample.getName());
		LOG.info(result);
		return result;
	}

	@RequestMapping(value = "/config/b", method = RequestMethod.GET)
	public String b() {
		String result = String.format("from application:%s, host-ip: %s, configurationPropertiesSample: %s",
				applicationName, ip, configurationPropertiesSample.getName());
		LOG.info(result);
		return result;
	}

}
