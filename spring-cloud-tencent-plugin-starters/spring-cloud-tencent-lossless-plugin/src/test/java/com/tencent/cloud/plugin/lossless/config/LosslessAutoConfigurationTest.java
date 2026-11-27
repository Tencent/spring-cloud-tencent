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

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tencent.cloud.plugin.lossless.LosslessRegistryAspect;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineActionManager;
import com.tencent.cloud.plugin.lossless.extension.PolarisLosslessCandidate;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link LosslessAutoConfiguration}.
 *
 * @author evelynwei
 */
@DisplayName("LosslessAutoConfiguration")
class LosslessAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LosslessPropertiesAutoConfiguration.class,
					LosslessAutoConfiguration.class))
			.withUserConfiguration(RequiredBeansConfiguration.class)
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=false");

	/**
	 * Verify disabled lossless does not require a service registry candidate.
	 */
	@DisplayName("test disabled lossless without registry")
	@Test
	void testDisabledLosslessWithoutRegistry() {
		// Act and assert
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(LosslessOfflineActionManager.class);
			assertThat(context).doesNotHaveBean(LosslessRegistryAspect.class);
			assertThat(context).hasSingleBean(LosslessProperties.class);
		});
	}

	/**
	 * Verify enabled lossless still reports a missing service registry candidate.
	 */
	@DisplayName("test enabled lossless without registry")
	@Test
	void testEnabledLosslessWithoutRegistry() {
		// Act and assert
		contextRunner.withPropertyValues("spring.cloud.polaris.lossless.enabled=true").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseMessage(
					"No candidate service registry is available for lossless.");
		});
	}

	/**
	 * Verify JDK proxies are selected by their ultimate target classes.
	 */
	@DisplayName("test jdk proxy target selection")
	@Test
	void testJdkProxyTargetSelection() throws Exception {
		// Arrange
		ServiceRegistry<Registration> polarisRegistryProxy = createJdkProxy(new PolarisServiceRegistry());
		Registration polarisRegistrationProxy = createJdkProxy(new PolarisRegistration());
		RegistrationTransformer polarisTransformerProxy = createJdkProxy(new PolarisRegistrationTransformer());
		List<ServiceRegistry> registries = new ArrayList<>();
		registries.add(polarisRegistryProxy);
		registries.add(new OtherServiceRegistry());
		List<Registration> registrations = new ArrayList<>();
		registrations.add(polarisRegistrationProxy);
		registrations.add(new OtherRegistration());
		List<RegistrationTransformer> transformers = new ArrayList<>();
		transformers.add(polarisTransformerProxy);
		transformers.add(new OtherRegistrationTransformer());

		// Act
		LosslessRegistryAspect aspect = new LosslessAutoConfiguration().losslessRegistryAspect(registries,
				registrations, transformers, new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(aspect, "serviceRegistry")).isSameAs(polarisRegistryProxy);
		assertThat(getField(aspect, "registration")).isSameAs(polarisRegistrationProxy);
		assertThat(getField(aspect, "registrationTransformer")).isSameAs(polarisTransformerProxy);
	}

	/**
	 * Verify a Polaris registry subtype is selected without relying on its class name.
	 */
	@DisplayName("test polaris registry subtype target selection")
	@Test
	void testPolarisRegistrySubtypeTargetSelection() throws Exception {
		// Arrange
		BusinessServiceRegistry businessRegistry = new BusinessServiceRegistry();
		List<ServiceRegistry> registries = new ArrayList<>();
		registries.add(businessRegistry);
		registries.add(new OtherServiceRegistry());
		List<Registration> registrations = new ArrayList<>();
		registrations.add(new PolarisRegistration());
		List<RegistrationTransformer> transformers = new ArrayList<>();
		transformers.add(new PolarisRegistrationTransformer());

		// Act
		LosslessRegistryAspect aspect = new LosslessAutoConfiguration().losslessRegistryAspect(registries,
				registrations, transformers, new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(aspect, "serviceRegistry")).isSameAs(businessRegistry);
	}

	/**
	 * Verify a CGLIB proxy of a Polaris registry subtype is selected after another candidate.
	 */
	@DisplayName("test polaris registry subtype cglib proxy target selection")
	@Test
	void testPolarisRegistrySubtypeCglibProxyTargetSelection() throws Exception {
		// Arrange
		BusinessServiceRegistry target = new BusinessServiceRegistry();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass(true);
		BusinessServiceRegistry businessRegistry = (BusinessServiceRegistry) proxyFactory.getProxy();
		List<ServiceRegistry> registries = new ArrayList<>();
		registries.add(new OtherServiceRegistry());
		registries.add(businessRegistry);
		List<Registration> registrations = new ArrayList<>();
		registrations.add(new PolarisRegistration());
		List<RegistrationTransformer> transformers = new ArrayList<>();
		transformers.add(new PolarisRegistrationTransformer());

		// Act
		LosslessRegistryAspect aspect = new LosslessAutoConfiguration().losslessRegistryAspect(registries,
				registrations, transformers, new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(aspect, "serviceRegistry")).isSameAs(businessRegistry);
	}

	/**
	 * Verify only Polaris-backed registries require the strict deregistration bridge.
	 */
	@DisplayName("test strict deregistration requirement")
	@Test
	void testStrictDeregistrationRequirement() throws Exception {
		// Arrange and act
		LosslessRegistryAspect polarisAspect = new LosslessAutoConfiguration().losslessRegistryAspect(
				List.of(new PolarisServiceRegistry()), List.of(new PolarisRegistration()),
				List.of(new PolarisRegistrationTransformer()), new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));
		LosslessRegistryAspect otherAspect = new LosslessAutoConfiguration().losslessRegistryAspect(
				List.of(new OtherServiceRegistry()), List.of(new OtherRegistration()),
				List.of(new OtherRegistrationTransformer()), new PolarisContextProperties(),
				new LosslessProperties(), Mockito.mock(PolarisSDKContextManager.class),
				Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(polarisAspect, "strictDeregistrationRequired")).isEqualTo(true);
		assertThat(getField(otherAspect, "strictDeregistrationRequired")).isEqualTo(false);
	}

	/**
	 * Verify all Spring-recognized boolean values keep binding and auto-configuration consistent.
	 */
	@DisplayName("test enabled boolean values")
	@Test
	void testEnabledBooleanValues() {
		for (String enabledValue : List.of("true", "on", "yes", "1")) {
			new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(LosslessPropertiesAutoConfiguration.class,
							LosslessAutoConfiguration.class))
					.withUserConfiguration(RequiredBeansConfiguration.class, RegistryBeansConfiguration.class)
					.withPropertyValues("spring.cloud.polaris.lossless.enabled=" + enabledValue)
					.run(context -> {
						assertThat(context).hasNotFailed();
						assertThat(context).hasSingleBean(LosslessProperties.class);
						assertThat(context.getBean(LosslessProperties.class).isEnabled()).isTrue();
						assertThat(context).hasSingleBean(LosslessOfflineActionManager.class);
						assertThat(context).hasSingleBean(LosslessRegistryAspect.class);
					});
		}
		for (String disabledValue : List.of("false", "off", "no", "0")) {
			new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(LosslessPropertiesAutoConfiguration.class,
							LosslessAutoConfiguration.class))
					.withUserConfiguration(RequiredBeansConfiguration.class, RegistryBeansConfiguration.class)
					.withPropertyValues("spring.cloud.polaris.lossless.enabled=" + disabledValue)
					.run(context -> {
						assertThat(context).hasNotFailed();
						assertThat(context).hasSingleBean(LosslessProperties.class);
						assertThat(context.getBean(LosslessProperties.class).isEnabled()).isFalse();
						assertThat(context).doesNotHaveBean(LosslessOfflineActionManager.class);
						assertThat(context).doesNotHaveBean(LosslessRegistryAspect.class);
					});
		}
	}

	/**
	 * Verify a nested opaque JDK proxy still identifies the Polaris target.
	 */
	@DisplayName("test nested opaque jdk proxy target selection")
	@Test
	void testNestedOpaqueJdkProxyTargetSelection() throws Exception {
		// Arrange
		ServiceRegistry<Registration> nestedOpaqueProxy = createNestedOpaqueJdkProxy(new PolarisServiceRegistry());
		List<ServiceRegistry> registries = List.of(nestedOpaqueProxy);
		List<Registration> registrations = List.of(new PolarisRegistration());
		List<RegistrationTransformer> transformers = List.of(new PolarisRegistrationTransformer());

		// Act
		LosslessRegistryAspect aspect = new LosslessAutoConfiguration().losslessRegistryAspect(registries,
				registrations, transformers, new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(aspect, "serviceRegistry")).isSameAs(nestedOpaqueProxy);
		assertThat(getField(aspect, "strictDeregistrationRequired")).isEqualTo(true);
	}

	/**
	 * Verify candidate selection uses the target class without borrowing pooled targets.
	 */
	@DisplayName("test dynamic target source selection")
	@Test
	void testDynamicTargetSourceSelection() throws Exception {
		// Arrange
		CountingTargetSource targetSource = new CountingTargetSource(new PolarisServiceRegistry());
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		List<ServiceRegistry> registries = List.of(proxy, new OtherServiceRegistry());

		// Act
		LosslessRegistryAspect aspect = new LosslessAutoConfiguration().losslessRegistryAspect(registries,
				List.of(new PolarisRegistration()), List.of(new PolarisRegistrationTransformer()),
				new PolarisContextProperties(), new LosslessProperties(),
				Mockito.mock(PolarisSDKContextManager.class), Mockito.mock(LosslessOfflineActionManager.class));

		// Assert
		assertThat(getField(aspect, "serviceRegistry")).isSameAs(proxy);
		assertThat(getField(aspect, "strictDeregistrationRequired")).isEqualTo(true);
		assertThat(targetSource.getCount()).isZero();
		assertThat(targetSource.getReleaseCount()).isZero();
	}

	private static <T> T createJdkProxy(T target) {
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass(false);
		return (T) proxyFactory.getProxy();
	}

	private static ServiceRegistry<Registration> createNestedOpaqueJdkProxy(PolarisServiceRegistry target) {
		ProxyFactory innerProxyFactory = new ProxyFactory(target);
		innerProxyFactory.setInterfaces(ServiceRegistry.class);
		innerProxyFactory.setProxyTargetClass(false);
		innerProxyFactory.setOpaque(true);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> innerProxy = (ServiceRegistry<Registration>) innerProxyFactory.getProxy();

		ProxyFactory outerProxyFactory = new ProxyFactory(innerProxy);
		outerProxyFactory.setInterfaces(ServiceRegistry.class);
		outerProxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> outerProxy = (ServiceRegistry<Registration>) outerProxyFactory.getProxy();
		return outerProxy;
	}

	private static Object getField(LosslessRegistryAspect aspect, String fieldName) throws Exception {
		Field field = LosslessRegistryAspect.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(aspect);
	}

	private static final class CountingTargetSource implements TargetSource {

		private final Object target;

		private int count;

		private int releaseCount;

		private CountingTargetSource(Object target) {
			this.target = target;
		}

		private int getCount() {
			return count;
		}

		private int getReleaseCount() {
			return releaseCount;
		}

		@Override
		public Class<?> getTargetClass() {
			return target.getClass();
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public Object getTarget() {
			count++;
			return target;
		}

		@Override
		public void releaseTarget(Object target) {
			releaseCount++;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class RequiredBeansConfiguration {

		@Bean
		PolarisContextProperties polarisContextProperties() {
			return new PolarisContextProperties();
		}

		@Bean
		PolarisSDKContextManager polarisSDKContextManager() {
			return Mockito.mock(PolarisSDKContextManager.class);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class RegistryBeansConfiguration {

		@Bean
		ServiceRegistry<Registration> serviceRegistry() {
			return Mockito.mock(ServiceRegistry.class);
		}

		@Bean
		Registration registration() {
			return Mockito.mock(Registration.class);
		}

		@Bean
		RegistrationTransformer registrationTransformer() {
			RegistrationTransformer transformer = Mockito.mock(RegistrationTransformer.class);
			Mockito.when(transformer.getRegistry()).thenReturn("mock");
			return transformer;
		}

	}

	private static class PolarisServiceRegistry implements ServiceRegistry<Registration>, PolarisLosslessCandidate {

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
		}

		@Override
		public void close() {
		}

		@Override
		public void setStatus(Registration registration, String status) {
		}

		@Override
		public Object getStatus(Registration registration) {
			return null;
		}
	}

	static class BusinessServiceRegistry extends PolarisServiceRegistry {

		BusinessServiceRegistry() {
		}

	}

	private static final class OtherServiceRegistry implements ServiceRegistry<Registration> {

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
		}

		@Override
		public void close() {
		}

		@Override
		public void setStatus(Registration registration, String status) {
		}

		@Override
		public Object getStatus(Registration registration) {
			return null;
		}
	}

	private static final class PolarisRegistration implements Registration, PolarisLosslessCandidate {

		@Override
		public String getInstanceId() {
			return "polaris-instance";
		}

		@Override
		public String getServiceId() {
			return "test-service";
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return 48080;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public URI getUri() {
			return URI.create("http://127.0.0.1:48080");
		}

		@Override
		public Map<String, String> getMetadata() {
			return Map.of();
		}
	}

	private static final class OtherRegistration implements Registration {

		@Override
		public String getInstanceId() {
			return "other-instance";
		}

		@Override
		public String getServiceId() {
			return "test-service";
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return 48081;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public URI getUri() {
			return URI.create("http://127.0.0.1:48081");
		}

		@Override
		public Map<String, String> getMetadata() {
			return Map.of();
		}
	}

	private static final class PolarisRegistrationTransformer implements RegistrationTransformer {

		@Override
		public String getRegistry() {
			return "polaris";
		}
	}

	private static final class OtherRegistrationTransformer implements RegistrationTransformer {

		@Override
		public String getRegistry() {
			return "other";
		}
	}
}
