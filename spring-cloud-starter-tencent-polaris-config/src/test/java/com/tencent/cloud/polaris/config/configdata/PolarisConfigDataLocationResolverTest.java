package com.tencent.cloud.polaris.config.configdata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisConfigDataLocationResolver}.
 *
 * @author wlx
 * @date 2022/7/16 4:10 下午
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisConfigDataLocationResolverTest {

	private final PolarisConfigDataLocationResolver resolver = new PolarisConfigDataLocationResolver(new DeferredLogs());

	@Mock
	private ConfigDataLocationResolverContext context;

	private final MockEnvironment environment = new MockEnvironment();

	private final Binder environmentBinder = Binder.get(this.environment);

	@Test
	public void testIsResolvable() {
		when(context.getBinder()).thenReturn(environmentBinder);
		assertThat(
				this.resolver.isResolvable(this.context, ConfigDataLocation.of("configserver:")))
				.isFalse();
		assertThat(
				this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris:")))
				.isTrue();
		assertThat(
				this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris")))
				.isTrue();
	}

	@Test
	public void unEnabledPolarisConfigData() {
		environment.setProperty("spring.cloud.polaris.config.enabled", "false");
		when(context.getBinder()).thenReturn(environmentBinder);
		assertThat(
				this.resolver.isResolvable(this.context, ConfigDataLocation.of("polaris:")))
				.isFalse();
	}
}
