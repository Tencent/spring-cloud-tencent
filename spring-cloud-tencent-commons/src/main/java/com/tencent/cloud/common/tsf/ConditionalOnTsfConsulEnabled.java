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

package com.tencent.cloud.common.tsf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tencent.polaris.api.utils.StringUtils;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that if Polaris enabled.
 *
 * @author Haotian Zhang
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(ConditionalOnTsfConsulEnabled.OnTsfEnabledCondition.class)
public @interface ConditionalOnTsfConsulEnabled {

	class OnTsfEnabledCondition implements Condition {

		@Override
		public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
			Environment environment = conditionContext.getEnvironment();
			boolean tsfConsulEnable = false;

			String tsfAppId = environment.getProperty("tsf_app_id");
			if (StringUtils.isNotBlank(tsfAppId)) {
				String tsfConsulIp = environment.getProperty("tsf_consul_ip");
				String tsePolarisAddress = environment.getProperty("polaris_address");
				if (StringUtils.isBlank(tsePolarisAddress) && StringUtils.isNotBlank(environment.getProperty("spring.cloud.polaris.address"))) {
					tsePolarisAddress = environment.getProperty("spring.cloud.polaris.address");
				}
				tsfConsulEnable = StringUtils.isNotBlank(tsfConsulIp) && StringUtils.isBlank(tsePolarisAddress);
			}

			return tsfConsulEnable;
		}
	}
}
