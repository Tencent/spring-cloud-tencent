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

package com.tencent.cloud.rpc.enhancement.audit.config;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import com.tencent.polaris.plugins.stat.audit.AuditLogConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AuditLogConfigModifier}.
 *
 * @author Yuwei Fu
 */
public class AuditLogConfigModifierTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisAuditLogPropertiesAutoConfiguration.class))
			.withPropertyValues("spring.cloud.polaris.enabled=true")
			.withPropertyValues("spring.application.name=audit-test")
			.withPropertyValues("spring.cloud.gateway.enabled=false");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	void testModify() {
		PolarisAuditLogProperties properties = new PolarisAuditLogProperties();
		properties.setEnabled(true);
		properties.setFormat("json");
		ConfigurationImpl configuration = mock(ConfigurationImpl.class);
		GlobalConfigImpl globalConfig = mock(GlobalConfigImpl.class);
		StatReporterConfigImpl statReporterConfig = mock(StatReporterConfigImpl.class);
		AuditLogConfig auditLogConfig = new AuditLogConfig();
		when(configuration.getGlobal()).thenReturn(globalConfig);
		when(globalConfig.getStatReporter()).thenReturn(statReporterConfig);
		when(statReporterConfig.getPluginConfig(
				StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG, AuditLogConfig.class))
				.thenReturn(auditLogConfig);

		AuditLogConfigModifier modifier = new AuditLogConfigModifier(properties);
		modifier.modify(configuration);

		assertThat(auditLogConfig.isEnable()).isTrue();
		assertThat(auditLogConfig.getFormat()).isEqualTo("json");
		assertThat(modifier.getOrder()).isEqualTo(OrderConstant.Modifier.AUDIT_LOG_ORDER);
	}

	@Test
	void testModifySdkConfiguration() {
		contextRunner
				.withPropertyValues("spring.cloud.polaris.audit-log.enabled=true")
				.withPropertyValues("spring.cloud.polaris.audit-log.format=json")
				.run(context -> {
					PolarisSDKContextManager contextManager = context.getBean(PolarisSDKContextManager.class);
					StatReporterConfig statReporter = contextManager.getSDKContext().getConfig()
							.getGlobal().getStatReporter();
					AuditLogConfig auditLogConfig = statReporter.getPluginConfig(
							StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG, AuditLogConfig.class);
					assertThat(auditLogConfig.isEnable()).isTrue();
					assertThat(auditLogConfig.getFormat()).isEqualTo("json");
				});
	}

	@Test
	void testAuditLogDisabledByDefault() {
		contextRunner.run(context -> {
			PolarisSDKContextManager contextManager = context.getBean(PolarisSDKContextManager.class);
			StatReporterConfig statReporter = contextManager.getSDKContext().getConfig()
					.getGlobal().getStatReporter();
			AuditLogConfig auditLogConfig = statReporter.getPluginConfig(
					StatReporterConfig.DEFAULT_REPORTER_AUDIT_LOG, AuditLogConfig.class);
			assertThat(auditLogConfig.isEnable()).isFalse();
			assertThat(auditLogConfig.getFormat()).isEqualTo("json");
		});
	}
}
