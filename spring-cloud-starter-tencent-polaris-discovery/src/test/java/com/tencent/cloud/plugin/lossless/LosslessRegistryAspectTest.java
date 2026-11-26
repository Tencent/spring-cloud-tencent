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

package com.tencent.cloud.plugin.lossless;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.OkHttpUtil;
import com.tencent.cloud.plugin.lossless.config.LosslessAutoConfiguration;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.plugin.lossless.config.LosslessPropertiesAutoConfiguration;
import com.tencent.cloud.plugin.lossless.extension.LosslessDeregistration;
import com.tencent.cloud.plugin.lossless.extension.LosslessDeregistrationResult;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineActionManager;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineCallback;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisServiceRegistry;
import com.tencent.cloud.rpc.enhancement.transformer.RegistrationTransformer;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugin.lossless.common.HttpLosslessActionProvider;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationUtils;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test for {@link LosslessRegistryAspect}.
 *
 * @author Shedfree Wu
 */
public class LosslessRegistryAspectTest {

	private static String NAMESPACE_TEST = "Test";

	private static String SERVICE_PROVIDER = "java_provider_test";

	private static String HOST = "127.0.0.1";

	private static int APPLICATION_PORT = 19091;

	private static int LOSSLESS_PORT_1 = 28083;

	private static NamingServer namingServer;

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.delayRegisterInterval=5000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=")
			.withPropertyValues("spring.cloud.polaris.admin.port=" + LOSSLESS_PORT_1)
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private final WebApplicationContextRunner contextRunner2 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=1000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/test")
			.withPropertyValues("spring.cloud.polaris.admin.port=28082")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private final WebApplicationContextRunner contextRunner3 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MockDiscoveryConfiguration.class,
					LosslessAutoConfiguration.class,
					LosslessPropertiesAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckInterval=1000")
			.withPropertyValues("spring.cloud.polaris.lossless.healthCheckPath=/test")
			.withPropertyValues("spring.cloud.polaris.admin.port=28082")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx")
			.withPropertyValues("spring.autoconfigure.exclude="
					+ "org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration");

	private final WebApplicationContextRunner contextRunner4 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					LosslessAutoConfiguration.class,
					LosslessPropertiesAutoConfiguration.class,
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withUserConfiguration(NamespaceCallbackConfiguration.class)
			.withPropertyValues("spring.cloud.nacos.discovery.enabled=false")
			.withPropertyValues("spring.cloud.polaris.lossless.enabled=true")
			.withPropertyValues("spring.cloud.polaris.admin.port=28085")
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.localIpAddress=" + HOST)
			.withPropertyValues("spring.cloud.polaris.localPort=" + APPLICATION_PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.namespace=shared")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=business")
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");


	@BeforeAll
	static void beforeAll() throws Exception {
		namingServer = NamingServer.startNamingServer(10081);

		// add service
		namingServer.getNamingService().addService(new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER));
	}

	@AfterAll
	static void afterAll() {
		if (null != namingServer) {
			namingServer.terminate();
		}
	}

	@BeforeEach
	void setUp() {
		PolarisSDKContextManager.innerDestroy();
	}

	@Test
	public void testRegister() {
		this.contextRunner.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();
			Thread.sleep(1000);
			// before register online status is false
			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/online", Collections.EMPTY_MAP)).isFalse();
			}).doesNotThrowAnyException();
			// delay register after 10s
			Thread.sleep(10000);
			PolarisServiceRegistry registry = context.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = context.getBean(PolarisRegistration.class);

			assertThatCode(() -> {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/online", Collections.EMPTY_MAP)).isTrue();
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(OkHttpUtil.checkUrl(HOST, LOSSLESS_PORT_1, "/offline", Collections.EMPTY_MAP)).isTrue();
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}).doesNotThrowAnyException();
		});
	}

	@Test
	public void testRegister2() {
		this.contextRunner2.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();

			Thread.sleep(2000);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();
		});
	}

	@Test
	public void testRegister3() {
		this.contextRunner3.run(context -> {

			AbstractAutoServiceRegistration autoServiceRegistration = context.getBean(AbstractAutoServiceRegistration.class);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.register(autoServiceRegistration);
			}).doesNotThrowAnyException();

			Thread.sleep(2000);

			assertThatCode(() -> {
				AutoServiceRegistrationUtils.deRegister(autoServiceRegistration);
			}).doesNotThrowAnyException();

			LosslessRegistryAspect losslessRegistryAspect = context.getBean(LosslessRegistryAspect.class);
			Field field = LosslessRegistryAspect.class.getDeclaredField("registrationTransformer");
			field.setAccessible(true);
			RegistrationTransformer registrationTransformer = (RegistrationTransformer) field.get(losslessRegistryAspect);
			assertThat(registrationTransformer.getClass().getName().contains("PolarisRegistrationTransformer"));

			field = LosslessRegistryAspect.class.getDeclaredField("registration");
			field.setAccessible(true);
			Registration registration = (Registration) field.get(losslessRegistryAspect);
			assertThat(registration.getClass().getName().contains("PolarisRegistration"));

			field = LosslessRegistryAspect.class.getDeclaredField("serviceRegistry");
			field.setAccessible(true);
			ServiceRegistry serviceRegistry = (ServiceRegistry) field.get(losslessRegistryAspect);
			assertThat(serviceRegistry.getClass().getName().contains("PolarisServiceRegistry"));
		});
	}

	/**
	 * Verify the callback namespace follows the discovery namespace used by Polaris registration.
	 */
	@Test
	public void testCallbackNamespaceUsesDiscoveryNamespace() {
		// Arrange
		AtomicReference<String> callbackNamespace = new AtomicReference<>();
		NamespaceCallbackConfiguration.namespaceCallback = context -> callbackNamespace.set(context.getNamespace());
		String previousNamespace = MetadataContext.LOCAL_NAMESPACE;
		MetadataContext.LOCAL_NAMESPACE = "shared";
		try {
			contextRunner4.run(context -> {
				PolarisRegistration registration = context.getBean(PolarisRegistration.class);
				RegistrationTransformer transformer = context.getBean(RegistrationTransformer.class);
				BaseInstance instance = transformer.transform(registration);
				LosslessOfflineActionManager manager = context.getBean(LosslessOfflineActionManager.class);
				@SuppressWarnings("unchecked")
				ServiceRegistry<Registration> registry = Mockito.mock(ServiceRegistry.class);

				// Act
				manager.createDeregisterAction(registration, registry, instance,
							(currentRegistration, deregistration) -> {
								deregistration.run();
								return LosslessDeregistrationResult.success();
							}).run();

				// Assert
				assertThat(callbackNamespace.get()).isEqualTo("business");
			});
		}
		finally {
			MetadataContext.LOCAL_NAMESPACE = previousNamespace;
			NamespaceCallbackConfiguration.namespaceCallback = null;
		}
	}

	/**
	 * Verify a register call for another registry is not intercepted by the Polaris aspect.
	 */
	@Test
	public void testInvokeRegisterSkipsNonTargetRegistry() throws Throwable {
		// Arrange
		ServiceRegistry<Registration> targetRegistry = Mockito.mock(ServiceRegistry.class);
		ServiceRegistry<Registration> otherRegistry = Mockito.mock(ServiceRegistry.class);
		Registration targetRegistration = Mockito.mock(Registration.class);
		Registration otherRegistration = Mockito.mock(Registration.class);
		RegistrationTransformer registrationTransformer = Mockito.mock(RegistrationTransformer.class);
		PolarisSDKContextManager polarisSDKContextManager = Mockito.mock(PolarisSDKContextManager.class);
		LosslessOfflineActionManager actionManager = Mockito.mock(LosslessOfflineActionManager.class);
		ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
		LosslessProperties losslessProperties = new LosslessProperties();
		losslessProperties.setEnabled(true);
		LosslessRegistryAspect aspect = new LosslessRegistryAspect(targetRegistry, targetRegistration, null,
				losslessProperties, polarisSDKContextManager, registrationTransformer, actionManager);
		Mockito.when(joinPoint.getTarget()).thenReturn(otherRegistry);
		Mockito.when(joinPoint.getArgs()).thenReturn(new Registration[] { otherRegistration });
		Mockito.when(joinPoint.proceed()).thenReturn(null);

		// Act
		Object result = aspect.invokeRegister(joinPoint);

		// Assert
		assertThat(result).isNull();
		Mockito.verify(joinPoint).proceed();
		Mockito.verifyNoInteractions(actionManager);
	}

	/**
	 * Verify a target registry behind nested singleton proxies still installs the lossless provider.
	 */
	@Test
	public void testInvokeRegisterMatchesNestedSingletonProxy() throws Throwable {
		// Arrange
		RecordingRegistry targetRegistry = new RecordingRegistry();
		Registration registration = Mockito.mock(Registration.class);
		RegistrationTransformer registrationTransformer = Mockito.mock(RegistrationTransformer.class);
		Instance instance = Mockito.mock(Instance.class);
		Mockito.when(registrationTransformer.transform(registration)).thenReturn(instance);
		PolarisSDKContextManager polarisSDKContextManager = Mockito.mock(PolarisSDKContextManager.class,
				Mockito.RETURNS_DEEP_STUBS);
		LosslessOfflineActionManager actionManager = Mockito.mock(LosslessOfflineActionManager.class);
		Mockito.doReturn((Runnable) () -> {
		}).when(actionManager).createDeregisterAction(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.anyBoolean());
		LosslessProperties losslessProperties = new LosslessProperties();
		losslessProperties.setEnabled(true);
		LosslessRegistryAspect aspect = new LosslessRegistryAspect(null, registration, null,
				losslessProperties, polarisSDKContextManager, registrationTransformer, actionManager);

		AspectJProxyFactory aspectProxyFactory = new AspectJProxyFactory(targetRegistry);
		aspectProxyFactory.addAspect(aspect);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> aspectProxy = aspectProxyFactory.getProxy();
		ProxyFactory outerProxyFactory = new ProxyFactory();
		outerProxyFactory.setTarget(aspectProxy);
		outerProxyFactory.setInterfaces(ServiceRegistry.class);
		outerProxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> outerProxy = (ServiceRegistry<Registration>) outerProxyFactory.getProxy();
		setServiceRegistry(aspect, outerProxy);

		// Act
		outerProxy.register(registration);

		// Assert
		Mockito.verify(actionManager).createDeregisterAction(registration, outerProxy, instance, targetRegistry, true);
		Mockito.verify(polarisSDKContextManager.getLosslessAPI()).setLosslessActionProvider(Mockito.eq(instance),
				Mockito.any(HttpLosslessActionProvider.class));
		Mockito.verify(polarisSDKContextManager.getLosslessAPI()).losslessRegister(instance);
	}

	/**
	 * Verify selected proxies that cannot be unwrapped through singleton targets still install the provider.
	 */
	@Test
	public void testInvokeRegisterMatchesOpaqueAndNestedProxies() throws Throwable {
		// Act and assert
		for (String proxyType : List.of("opaque", "hot-swappable", "nested-opaque")) {
			runRegisterThroughSelectedProxy(proxyType);
		}
	}

	private static void runRegisterThroughSelectedProxy(String proxyType) throws Throwable {
		// Arrange
		ProxyableRegistry targetRegistry = new ProxyableRegistry();
		Registration registration = Mockito.mock(Registration.class);
		RegistrationTransformer registrationTransformer = Mockito.mock(RegistrationTransformer.class);
		Instance instance = Mockito.mock(Instance.class);
		Mockito.when(registrationTransformer.transform(registration)).thenReturn(instance);
		PolarisSDKContextManager polarisSDKContextManager = Mockito.mock(PolarisSDKContextManager.class,
				Mockito.RETURNS_DEEP_STUBS);
		LosslessOfflineActionManager actionManager = Mockito.mock(LosslessOfflineActionManager.class);
		Mockito.doReturn((Runnable) () -> {
		}).when(actionManager).createDeregisterAction(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.anyBoolean());
		LosslessProperties losslessProperties = new LosslessProperties();
		losslessProperties.setEnabled(true);
		LosslessRegistryAspect aspect = new LosslessRegistryAspect(null, registration, null,
				losslessProperties, polarisSDKContextManager, registrationTransformer, actionManager);

		AspectJProxyFactory aspectProxyFactory;
		if ("opaque".equals(proxyType)) {
			aspectProxyFactory = new AspectJProxyFactory(targetRegistry);
			aspectProxyFactory.setProxyTargetClass(true);
			aspectProxyFactory.setOpaque(true);
		}
		else if ("nested-opaque".equals(proxyType)) {
			ProxyFactory innerProxyFactory = new ProxyFactory(targetRegistry);
			innerProxyFactory.setInterfaces(ServiceRegistry.class);
			innerProxyFactory.setProxyTargetClass(false);
			innerProxyFactory.setOpaque(true);
			@SuppressWarnings("unchecked")
			ServiceRegistry<Registration> innerProxy = (ServiceRegistry<Registration>) innerProxyFactory.getProxy();
			aspectProxyFactory = new AspectJProxyFactory(innerProxy);
			aspectProxyFactory.setInterfaces(ServiceRegistry.class);
			aspectProxyFactory.setProxyTargetClass(false);
		}
		else {
			aspectProxyFactory = new AspectJProxyFactory();
			aspectProxyFactory.setTargetSource(new HotSwappableTargetSource(targetRegistry));
			aspectProxyFactory.setInterfaces(ServiceRegistry.class);
		}
		aspectProxyFactory.addAspect(aspect);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> selectedProxy = (ServiceRegistry<Registration>) aspectProxyFactory.getProxy();
		setServiceRegistry(aspect, selectedProxy);

		// Act
		selectedProxy.register(registration);

		// Assert
		Mockito.verify(actionManager).createDeregisterAction(registration, selectedProxy, instance, targetRegistry, true);
		Mockito.verify(polarisSDKContextManager.getLosslessAPI()).setLosslessActionProvider(Mockito.eq(instance),
				Mockito.any(HttpLosslessActionProvider.class));
		Mockito.verify(polarisSDKContextManager.getLosslessAPI()).losslessRegister(instance);
	}

	private static void setServiceRegistry(LosslessRegistryAspect aspect,
			ServiceRegistry<Registration> serviceRegistry) throws ReflectiveOperationException {
		Field field = LosslessRegistryAspect.class.getDeclaredField("serviceRegistry");
		field.setAccessible(true);
		field.set(aspect, serviceRegistry);
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisPropertiesConfiguration {

	}

	@Configuration
	static class MockDiscoveryConfiguration {
		@Bean
		public ServiceRegistry mockServiceRegistry() {
			return new ServiceRegistry() {
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
			};
		}

		@Bean
		public Registration mockRegistration() {
			return new Registration() {
				@Override
				public String getServiceId() {
					return null;
				}

				@Override
				public String getHost() {
					return null;
				}

				@Override
				public int getPort() {
					return 0;
				}

				@Override
				public boolean isSecure() {
					return false;
				}

				@Override
				public URI getUri() {
					return null;
				}

				@Override
				public Map<String, String> getMetadata() {
					return null;
				}
			};
		}

		@Bean
		public RegistrationTransformer mockRegistrationTransformer() {
			return new RegistrationTransformer() {
				@Override
				public String getRegistry() {
					return null;
				}
			};
		}
	}

	private static class RecordingRegistry implements ServiceRegistry<Registration>, LosslessDeregistration {

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

		@Override
		public LosslessDeregistrationResult deregisterForLossless(Registration registration, Runnable deregistration) {
			deregistration.run();
			return LosslessDeregistrationResult.success();
		}
	}

	public static class ProxyableRegistry implements ServiceRegistry<Registration>, LosslessDeregistration {

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

		@Override
		public LosslessDeregistrationResult deregisterForLossless(Registration registration, Runnable deregistration) {
			deregistration.run();
			return LosslessDeregistrationResult.success();
		}
	}

	@Configuration
	static class NamespaceCallbackConfiguration {

		private static LosslessOfflineCallback namespaceCallback;

		@Bean
		public LosslessOfflineCallback callback() {
			return namespaceCallback;
		}
	}
}
