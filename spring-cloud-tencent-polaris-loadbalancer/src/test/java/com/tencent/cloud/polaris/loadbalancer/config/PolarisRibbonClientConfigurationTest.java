package com.tencent.cloud.polaris.loadbalancer.config;

import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisRibbonClientConfiguration}.
 *
 * @author wlx
 * @date 2022/7/2 10:36 上午
 */
public class PolarisRibbonClientConfigurationTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	@Test
	public void testDefaultInitialization() {
		this.applicationContextRunner
				.withConfiguration(AutoConfigurations.of(
						TestApplication.class,
						PolarisRibbonClientConfiguration.class))
				.run(context -> {
					assertThat(context).hasSingleBean(PolarisRibbonClientConfiguration.class);
					assertThat(context).hasSingleBean(PolarisLoadBalancer.class);
				});
	}

	@SpringBootApplication
	static class TestApplication {

	}

}
