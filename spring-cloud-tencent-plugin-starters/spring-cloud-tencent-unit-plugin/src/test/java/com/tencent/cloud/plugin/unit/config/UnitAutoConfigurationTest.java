package com.tencent.cloud.plugin.unit.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;


public class UnitAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(UnitAutoConfiguration.class))
			.withPropertyValues(
					"tsf_consul_ip=localhost"
			);

	@Test
	void shouldCreateBeansWhenConditionsMet() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(UnitBeanPostProcessor.class);
		});
	}
}
