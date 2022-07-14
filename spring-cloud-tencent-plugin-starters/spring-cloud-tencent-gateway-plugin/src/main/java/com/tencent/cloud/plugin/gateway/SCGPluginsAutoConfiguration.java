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

package com.tencent.cloud.plugin.gateway;

import java.util.List;

import com.tencent.cloud.plugin.gateway.staining.StainingProperties;
import com.tencent.cloud.plugin.gateway.staining.TrafficStainer;
import com.tencent.cloud.plugin.gateway.staining.TrafficStainingGatewayFilter;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingExecutor;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingProperties;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleTrafficStainer;
import com.tencent.cloud.plugin.gateway.staining.rule.StainingRuleManager;
import com.tencent.polaris.configuration.api.core.ConfigFileService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for spring cloud gateway plugins.
 * @author lepdou 2022-07-06
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.tencent.plugin.scg.enabled", matchIfMissing = true)
public class SCGPluginsAutoConfiguration {

	@Configuration
	@ConditionalOnProperty("spring.cloud.tencent.plugin.scg.staining.enabled")
	public static class StainingPluginConfiguration {

		@Bean
		public StainingProperties stainingProperties() {
			return new StainingProperties();
		}

		@Configuration
		@ConditionalOnProperty(value = "spring.cloud.tencent.plugin.scg.staining.rule-staining.enabled", matchIfMissing = true)
		public static class RuleStainingPluginConfiguration {

			@Bean
			public RuleStainingProperties ruleStainingProperties() {
				return new RuleStainingProperties();
			}

			@Bean
			public StainingRuleManager stainingRuleManager(RuleStainingProperties stainingProperties, ConfigFileService configFileService) {
				return new StainingRuleManager(stainingProperties, configFileService);
			}

			@Bean
			public TrafficStainingGatewayFilter trafficStainingGatewayFilter(List<TrafficStainer> trafficStainer) {
				return new TrafficStainingGatewayFilter(trafficStainer);
			}

			@Bean
			public RuleStainingExecutor ruleStainingExecutor() {
				return new RuleStainingExecutor();
			}

			@Bean
			public RuleTrafficStainer ruleTrafficStainer(StainingRuleManager stainingRuleManager,
					RuleStainingExecutor ruleStainingExecutor) {
				return new RuleTrafficStainer(stainingRuleManager, ruleStainingExecutor);
			}
		}
	}
}
