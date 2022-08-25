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

package com.tencent.cloud.plugin.gateway.staining.rule;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch staining rule from polaris, and deserialize to {@link StainingRule}.
 * @author lepdou 2022-07-07
 */
public class StainingRuleManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(StainingRuleManager.class);

	private final RuleStainingProperties stainingProperties;
	private final ConfigFileService configFileService;

	private StainingRule stainingRule;

	public StainingRuleManager(RuleStainingProperties stainingProperties, ConfigFileService configFileService) {
		this.stainingProperties = stainingProperties;
		this.configFileService = configFileService;

		initStainingRule();
	}

	private void initStainingRule() {
		ConfigFile rulesFile = configFileService.getConfigFile(stainingProperties.getNamespace(), stainingProperties.getGroup(),
				stainingProperties.getFileName());

		rulesFile.addChangeListener(event -> {
			LOGGER.info("[SCT] update scg staining rules. {}", event);
			deserialize(event.getNewValue());
		});

		String ruleJson = rulesFile.getContent();
		LOGGER.info("[SCT] init scg staining rules. {}", ruleJson);

		deserialize(ruleJson);
	}

	private void deserialize(String ruleJsonStr) {
		if (StringUtils.isBlank(ruleJsonStr)) {
			stainingRule = null;
			return;
		}

		try {
			stainingRule = JacksonUtils.deserialize(ruleJsonStr, StainingRule.class);
		}
		catch (Exception e) {
			LOGGER.error("[SCT] deserialize staining rule error.", e);
			throw e;
		}
	}

	public StainingRule getStainingRule() {
		return stainingRule;
	}
}
