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

package com.tencent.cloud.plugin.lossless.config;

import java.util.List;
import java.util.function.Predicate;

import com.tencent.cloud.plugin.lossless.LosslessRegistryAspect;
import com.tencent.cloud.plugin.lossless.extension.AopTargetUtils;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineActionManager;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineCallback;
import com.tencent.cloud.plugin.lossless.extension.PolarisLosslessCandidate;
import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Autoconfiguration of lossless.
 *
 * @author Shedfree Wu
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPolarisEnabled
@Import(LosslessPropertiesAutoConfiguration.class)
public class LosslessAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(LosslessEnabledCondition.class)
	public LosslessOfflineActionManager losslessOfflineActionManager(
			ObjectProvider<LosslessOfflineCallback> callbackProvider, ListableBeanFactory beanFactory,
			LosslessProperties losslessProperties) {
		return new LosslessOfflineActionManager(callbackProvider, beanFactory, losslessProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(LosslessEnabledCondition.class)
	public LosslessRegistryAspect losslessRegistryAspect(
			List<ServiceRegistry> serviceRegistryList, List<Registration> registrationList, List<RegistrationTransformer> registrationTransformerList,
			PolarisContextProperties properties, LosslessProperties losslessProperties,
			PolarisSDKContextManager polarisSDKContextManager, LosslessOfflineActionManager actionManager) {
		ServiceRegistry targetServiceRegistry = selectTarget(serviceRegistryList, "service registry",
				LosslessAutoConfiguration::isPolarisCandidate);
		Registration targetRegistration = selectTarget(registrationList, "registration",
				LosslessAutoConfiguration::isPolarisCandidate);
		RegistrationTransformer targetRegistrationTransformer = selectTarget(registrationTransformerList,
				"registration transformer", LosslessAutoConfiguration::isPolarisTransformer);
		return new LosslessRegistryAspect(targetServiceRegistry, targetRegistration, properties, losslessProperties,
				polarisSDKContextManager, targetRegistrationTransformer, actionManager,
				isPolarisCandidate(targetServiceRegistry));
	}

	private static <T> T selectTarget(List<T> candidates, String description, Predicate<T> candidateFilter) {
		if (candidates.isEmpty()) {
			throw new IllegalStateException("No candidate " + description + " is available for lossless.");
		}
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		for (T candidate : candidates) {
			if (candidateFilter.test(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Multiple " + description + " candidates are available, but no Polaris candidate can be selected.");
	}

	private static boolean isPolarisCandidate(Object candidate) {
		Class<?> targetClass = AopTargetUtils.getUltimateTargetClass(candidate);
		return targetClass != null && PolarisLosslessCandidate.class.isAssignableFrom(targetClass);
	}

	private static boolean isPolarisTransformer(RegistrationTransformer candidate) {
		return "polaris".equalsIgnoreCase(candidate.getRegistry());
	}

	static final class LosslessEnabledCondition implements Condition {

		private static final String LOSSLESS_ENABLED_PROPERTY = "spring.cloud.polaris.lossless.enabled";

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return context.getEnvironment()
					.getProperty(LOSSLESS_ENABLED_PROPERTY, Boolean.class, false);
		}

	}

}
