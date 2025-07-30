package com.tencent.cloud.polaris.ratelimit.config;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.polaris.api.config.provider.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class PolarisRateLimitConfigModifierTest {

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TestApplication.class))
			.withPropertyValues("spring.cloud.polaris.ratelimit.maxQueuingTime=500")
			.withPropertyValues("spring.cloud.polaris.ratelimit.limiterAddresses=127.0.0.1:8080,127.0.0.1:8081")
			.withPropertyValues("spring.cloud.polaris.ratelimit.remoteTaskInterval=50");

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testModify() {
		this.applicationContextRunner.run(context -> {
			PolarisSDKContextManager polarisSDKContextManager = context.getBean(PolarisSDKContextManager.class);
			RateLimitConfig config = polarisSDKContextManager.getSDKContext().
					getConfig().getProvider().getRateLimit();
			assertThat(config.isEnable()).isTrue();
			assertThat(config.getLimiterAddresses().get(0)).isEqualTo("127.0.0.1:8080");
			assertThat(config.getLimiterAddresses().get(1)).isEqualTo("127.0.0.1:8081");
			assertThat(config.getMaxQueuingTime()).isEqualTo(500);
			assertThat(config.getRemoteTaskIntervalMilli()).isEqualTo(50);
		});
	}


	@SpringBootApplication
	protected static class TestApplication {

	}
}
