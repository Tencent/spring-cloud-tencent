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

package com.tencent.cloud.polaris.registry;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.cloud.plugin.lossless.extension.LosslessDeregistration;
import com.tencent.cloud.plugin.lossless.extension.LosslessDeregistrationResult;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineActionManager;
import com.tencent.cloud.plugin.lossless.extension.LosslessOfflineCallback;
import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.cloud.polaris.context.config.PolarisContextAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryAutoConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryClientConfiguration;
import com.tencent.cloud.polaris.discovery.PolarisDiscoveryHandler;
import com.tencent.cloud.rpc.enhancement.stat.config.PolarisStatProperties;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.annotation.Configuration;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.PORT;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PolarisServiceRegistry}.
 *
 * @author Haotian Zhang
 */
public class PolarisServiceRegistryTest {

	private static NamingServer namingServer;

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx");

	private final WebApplicationContextRunner contextRunner2 = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					PolarisContextAutoConfiguration.class,
					PolarisPropertiesConfiguration.class,
					PolarisDiscoveryClientConfiguration.class,
					PolarisDiscoveryAutoConfiguration.class))
			.withPropertyValues("spring.application.name=" + SERVICE_PROVIDER)
			.withPropertyValues("server.port=" + PORT)
			.withPropertyValues("spring.cloud.polaris.address=grpc://127.0.0.1:10081")
			.withPropertyValues("spring.cloud.polaris.discovery.namespace=" + NAMESPACE_TEST)
			.withPropertyValues("spring.cloud.polaris.discovery.token=xxxxxx")
			.withPropertyValues("spring.cloud.polaris.discovery.health-check-url=/test");

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

	@AfterEach
	void tearDown() {
		PolarisSDKContextManager.isRegistered = false;
	}

	@Test
	public void testRegister() {
		this.contextRunner.run(context -> {
			PolarisServiceRegistry registry = context.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);
			when(registration.getHost()).thenReturn("127.0.0.1");
			when(registration.getPort()).thenReturn(PORT);
			when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);

			assertThatCode(() -> {
				registry.register(registration);
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				assertThat(registry.getStatus(registration)).isEqualTo("DOWN");
			}).doesNotThrowAnyException();

			assertThatCode(() -> {
				registry.deregister(registration);
			}).doesNotThrowAnyException();
		});
	}

	@Test
	public void testDeRegister() {
		this.contextRunner.run(context -> {
			PolarisServiceRegistry registry = context.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);
			doReturn(null).when(registration).getServiceId();
			assertThatCode(() -> {
				registry.deregister(registration);
			}).doesNotThrowAnyException();
		});
	}

	/**
	 * Verify that a lossless deregistration failure is propagated and the local state is not
	 * marked as deregistered.
	 */
	@Test
	void testDeregisterForLosslessWhenProviderFails() {
		PolarisDiscoveryProperties discoveryProperties = Mockito.mock(PolarisDiscoveryProperties.class);
		PolarisSDKContextManager contextManager = Mockito.mock(PolarisSDKContextManager.class);
		PolarisDiscoveryHandler discoveryHandler = Mockito.mock(PolarisDiscoveryHandler.class);
		StaticMetadataManager staticMetadataManager = Mockito.mock(StaticMetadataManager.class);
		PolarisStatProperties statProperties = Mockito.mock(PolarisStatProperties.class);
		ProviderAPI providerAPI = Mockito.mock(ProviderAPI.class);
		PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);
		RuntimeException failure = new RuntimeException("mock deregistration failure");

		when(discoveryProperties.getNamespace()).thenReturn(NAMESPACE_TEST);
		when(contextManager.getProviderAPI()).thenReturn(providerAPI);
		when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);
		when(registration.getHost()).thenReturn("127.0.0.1");
		when(registration.getPort()).thenReturn(PORT);
		when(registration.getInstanceId()).thenReturn("instance-id");
		doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		PolarisSDKContextManager.isRegistered = true;

		PolarisServiceRegistry registry = new PolarisServiceRegistry(discoveryProperties, contextManager,
				discoveryHandler, staticMetadataManager, statProperties);

		assertThat(registry).isInstanceOf(LosslessDeregistration.class);
			LosslessDeregistrationResult result = registry.deregisterForLossless(registration,
					() -> registry.deregister(registration));
			assertThat(result.getFailure()).isEqualTo(failure);
		assertThat(PolarisSDKContextManager.isRegistered).isTrue();

		PolarisSDKContextManager.isRegistered = false;
	}

	/**
	 * Verify the strict path still sends an idempotent request when the local state is false.
	 */
	@Test
	void testDeregisterForLosslessWhenLocalStateIsFalse() {
		PolarisDiscoveryProperties discoveryProperties = Mockito.mock(PolarisDiscoveryProperties.class);
		PolarisSDKContextManager contextManager = Mockito.mock(PolarisSDKContextManager.class);
		PolarisDiscoveryHandler discoveryHandler = Mockito.mock(PolarisDiscoveryHandler.class);
		StaticMetadataManager staticMetadataManager = Mockito.mock(StaticMetadataManager.class);
		PolarisStatProperties statProperties = Mockito.mock(PolarisStatProperties.class);
		ProviderAPI providerAPI = Mockito.mock(ProviderAPI.class);
		PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);

		when(discoveryProperties.getNamespace()).thenReturn(NAMESPACE_TEST);
		when(contextManager.getProviderAPI()).thenReturn(providerAPI);
		when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);
		when(registration.getHost()).thenReturn("127.0.0.1");
		when(registration.getPort()).thenReturn(PORT);
		when(registration.getInstanceId()).thenReturn("instance-id");
		PolarisSDKContextManager.isRegistered = false;

		PolarisServiceRegistry registry = new PolarisServiceRegistry(discoveryProperties, contextManager,
				discoveryHandler, staticMetadataManager, statProperties);

			LosslessDeregistrationResult result = registry.deregisterForLossless(registration,
					() -> registry.deregister(registration));
			assertThat(result.getFailure()).isNull();

		Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		assertThat(PolarisSDKContextManager.isRegistered).isFalse();
	}

	/**
	 * Verify normal deregistration keeps the legacy behavior of recording provider failures.
	 */
	@Test
	void testNormalDeregisterRecordsProviderFailure() {
		PolarisDiscoveryProperties discoveryProperties = Mockito.mock(PolarisDiscoveryProperties.class);
		PolarisSDKContextManager contextManager = Mockito.mock(PolarisSDKContextManager.class);
		PolarisDiscoveryHandler discoveryHandler = Mockito.mock(PolarisDiscoveryHandler.class);
		StaticMetadataManager staticMetadataManager = Mockito.mock(StaticMetadataManager.class);
		PolarisStatProperties statProperties = Mockito.mock(PolarisStatProperties.class);
		ProviderAPI providerAPI = Mockito.mock(ProviderAPI.class);
		PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);

		when(discoveryProperties.getNamespace()).thenReturn(NAMESPACE_TEST);
		when(contextManager.getProviderAPI()).thenReturn(providerAPI);
		when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);
		when(registration.getHost()).thenReturn("127.0.0.1");
		when(registration.getPort()).thenReturn(PORT);
		when(registration.getInstanceId()).thenReturn("instance-id");
		doThrow(new RuntimeException("mock deregistration failure"))
				.when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		PolarisSDKContextManager.isRegistered = true;

		PolarisServiceRegistry registry = new PolarisServiceRegistry(discoveryProperties, contextManager,
				discoveryHandler, staticMetadataManager, statProperties);

		assertThatCode(() -> registry.deregister(registration)).doesNotThrowAnyException();

		assertThat(PolarisSDKContextManager.isRegistered).isTrue();
	}

	/**
	 * Verify the lossless strict path still dispatches through a legacy deregister override.
	 */
	@Test
	void testDeregisterForLosslessInvokesLegacyOverride() {
		// Arrange
		TestPolarisServiceRegistry registry = createTestPolarisRegistry();
		ProviderAPI providerAPI = registry.getProviderAPI();
		PolarisRegistration registration = registry.getRegistration();
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		LosslessDeregistrationResult result = registry.deregisterForLossless(registration,
				() -> registry.deregister(registration));
		assertThat(result.getFailure()).isNull();
		assertThat(registry.legacyCleanupCalls).isEqualTo(1);
		Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		assertThat(PolarisSDKContextManager.isRegistered).isFalse();

		PolarisSDKContextManager.isRegistered = false;
	}

	/**
	 * Verify the lossless strict path keeps failures returned by a legacy deregister override.
	 */
	@Test
	void testDeregisterForLosslessPropagatesFailureFromLegacyOverride() {
		// Arrange
		TestPolarisServiceRegistry registry = createTestPolarisRegistry();
		ProviderAPI providerAPI = registry.getProviderAPI();
		PolarisRegistration registration = registry.getRegistration();
		RuntimeException failure = new IllegalStateException("legacy deregistration failure");
		doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		LosslessDeregistrationResult result = registry.deregisterForLossless(registration,
				() -> registry.deregister(registration));
		assertThat(result.getFailure()).isEqualTo(failure);
		assertThat(registry.legacyCleanupCalls).isEqualTo(1);
		assertThat(PolarisSDKContextManager.isRegistered).isTrue();

		PolarisSDKContextManager.isRegistered = false;
	}

	/**
	 * Verify a strict action fails when deregister advice swallows the provider failure.
	 */
	@Test
	void testLosslessActionFailsWhenDeregisterAdviceSwallowsFailure() throws Exception {
		// Act and assert
		for (String proxyType : List.of("cglib", "jdk-all", "jdk-restricted")) {
			runLosslessActionThroughSwallowingAdvice(proxyType);
		}
	}

	/**
	 * Verify a business subclass that swallows the provider failure cannot report action success.
	 */
	@Test
	void testDeregisterForLosslessFailsWhenLegacyOverrideSwallowsFailure() throws Exception {
		// Arrange
		SwallowingPolarisServiceRegistry registry = createSwallowingPolarisServiceRegistry();
		ProviderAPI providerAPI = registry.getProviderAPI();
		PolarisRegistration registration = registry.getRegistration();
		RuntimeException failure = new IllegalStateException("swallowed legacy deregistration failure");
		doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		PolarisSDKContextManager.isRegistered = true;

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
		ProxyFactory proxyFactory = new ProxyFactory(registry);
		proxyFactory.setProxyTargetClass(true);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();

		// Act and assert
		try {
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run()).isEqualTo(failure);
			assertThat(registry.legacyCleanupCalls).isEqualTo(1);
			assertThat(registry.swallowCleanupCalls).isEqualTo(1);
			assertThat(PolarisSDKContextManager.isRegistered).isTrue();
		}
		finally {
			manager.destroy();
			registry.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	/**
	 * Verify a strict action fails when deregister advice skips the underlying invocation.
	 */
	@Test
	void testLosslessActionFailsWhenDeregisterAdviceSkipsInvocation() throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if ("deregister".equals(invocation.getMethod().getName())) {
				return null;
			}
			return invocation.proceed();
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		try {
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run())
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Lossless deregistration was not executed.");
			Mockito.verify(providerAPI, Mockito.never())
					.deRegister(Mockito.any(InstanceDeregisterRequest.class));
			assertThat(PolarisSDKContextManager.isRegistered).isTrue();
		}
		finally {
			manager.destroy();
			target.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	/**
	 * Verify a previous normal deregistration does not leave a successful strict outcome.
	 */
	@Test
	void testLosslessActionIgnoresOutcomeFromPreviousNormalDeregistration() throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		PolarisSDKContextManager.isRegistered = true;
		target.deregister(registration);
		Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));

		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if ("deregister".equals(invocation.getMethod().getName())) {
				return null;
			}
			return invocation.proceed();
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		try {
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run())
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Lossless deregistration was not executed.");
			Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
			assertThat(PolarisSDKContextManager.isRegistered).isTrue();
		}
		finally {
			manager.destroy();
			target.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	/**
	 * Verify a CGLIB proxy preserves the legacy deregister override in the lossless path.
	 */
	@Test
	void testDeregisterForLosslessInvokesLegacyOverrideThroughProxy() {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(target);
		proxyFactory.setProxyTargetClass(true);
		TestPolarisServiceRegistry proxy = (TestPolarisServiceRegistry) proxyFactory.getProxy();
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
			LosslessDeregistrationResult result = proxy.deregisterForLossless(registration,
					() -> proxy.deregister(registration));
			assertThat(result.getFailure()).isNull();
		assertThat(target.legacyCleanupCalls).isEqualTo(1);
		Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		assertThat(PolarisSDKContextManager.isRegistered).isFalse();

		PolarisSDKContextManager.isRegistered = false;
	}

	/**
	 * Verify the strict lossless action keeps advice on the original deregister proxy method.
	 */
	@Test
	void testLosslessActionKeepsDeregisterAdvice() throws Exception {
		// Act and assert
		for (String proxyType : List.of("cglib", "jdk-all", "jdk-restricted")) {
			runLosslessActionThroughAdvisedProxy(proxyType);
		}
	}

	/**
	 * Verify cleanup after a deregister advice invocation still runs when the provider fails.
	 */
	@Test
	void testLosslessActionRunsPostDeregisterAdviceCleanupOnProviderFailure() throws Exception {
		// Act and assert
		for (boolean callbackEnabled : List.of(false, true)) {
			runLosslessActionThroughPostDeregisterAdvice(callbackEnabled);
		}
	}

	/**
	 * Verify strict failure propagation survives a synchronous thread switch in deregister advice.
	 */
	@Test
	void testLosslessActionPropagatesFailureThroughThreadSwitchingAdvice() throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		RuntimeException failure = new IllegalStateException("thread-switching deregistration failure");
		ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "test-deregister-advice"));
		try {
			doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
			ProxyFactory proxyFactory = new ProxyFactory(target);
			proxyFactory.setProxyTargetClass(true);
			proxyFactory.addAdvice((MethodInterceptor) invocation -> {
				if (!"deregister".equals(invocation.getMethod().getName())) {
					return invocation.proceed();
				}
				Future<Object> deregistration = executor.submit(() -> {
					try {
						return invocation.proceed();
					}
					catch (RuntimeException | Error e) {
						throw e;
					}
					catch (Throwable e) {
						throw new IllegalStateException("deregistration failed", e);
					}
				});
				try {
					return deregistration.get(5, TimeUnit.SECONDS);
				}
				catch (Exception e) {
					Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
					if (cause instanceof RuntimeException runtimeException) {
						throw runtimeException;
					}
					throw new IllegalStateException("deregistration advice failed", cause);
				}
			});
			@SuppressWarnings("unchecked")
			ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
					beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
			PolarisSDKContextManager.isRegistered = true;

			// Act and assert
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run()).isEqualTo(failure);
			assertThat(PolarisSDKContextManager.isRegistered).isTrue();
			Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		}
		finally {
			executor.shutdownNow();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	@Test
	void testHeartbeat() {
		this.contextRunner2.run(context -> {
			PolarisServiceRegistry registry = context.getBean(PolarisServiceRegistry.class);
			PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);
			when(registration.getHost()).thenReturn("127.0.0.1");
			when(registration.getPort()).thenReturn(8080);
			when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);

			assertThatCode(() -> registry.register(registration)).doesNotThrowAnyException();

			Thread.sleep(6000);

			assertThatCode(() -> registry.deregister(registration)).doesNotThrowAnyException();
		});
	}

	private static TestPolarisServiceRegistry createTestPolarisRegistry() {
		return createTestPolarisRegistry(false);
	}

	private static SwallowingPolarisServiceRegistry createSwallowingPolarisServiceRegistry() {
		return (SwallowingPolarisServiceRegistry) createTestPolarisRegistry(true);
	}

	private static TestPolarisServiceRegistry createTestPolarisRegistry(boolean swallowing) {
		PolarisDiscoveryProperties discoveryProperties = Mockito.mock(PolarisDiscoveryProperties.class);
		PolarisSDKContextManager contextManager = Mockito.mock(PolarisSDKContextManager.class);
		PolarisDiscoveryHandler discoveryHandler = Mockito.mock(PolarisDiscoveryHandler.class);
		StaticMetadataManager staticMetadataManager = Mockito.mock(StaticMetadataManager.class);
		PolarisStatProperties statProperties = Mockito.mock(PolarisStatProperties.class);
		ProviderAPI providerAPI = Mockito.mock(ProviderAPI.class);
		PolarisRegistration registration = Mockito.mock(PolarisRegistration.class);

		when(discoveryProperties.getNamespace()).thenReturn(NAMESPACE_TEST);
		when(contextManager.getProviderAPI()).thenReturn(providerAPI);
		when(registration.getServiceId()).thenReturn(SERVICE_PROVIDER);
		when(registration.getHost()).thenReturn("127.0.0.1");
		when(registration.getPort()).thenReturn(PORT);
		when(registration.getInstanceId()).thenReturn("instance-id");

		if (swallowing) {
			return new SwallowingPolarisServiceRegistry(discoveryProperties, contextManager, discoveryHandler,
					staticMetadataManager, statProperties, providerAPI, registration);
		}
		return new TestPolarisServiceRegistry(discoveryProperties, contextManager, discoveryHandler,
				staticMetadataManager, statProperties, providerAPI, registration);
	}

	private static void runLosslessActionThroughAdvisedProxy(String proxyType) throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		AtomicInteger deregisterAdviceCalls = new AtomicInteger();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass("cglib".equals(proxyType));
		if ("jdk-restricted".equals(proxyType)) {
			proxyFactory.setInterfaces(ServiceRegistry.class);
		}
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if ("deregister".equals(invocation.getMethod().getName())) {
				deregisterAdviceCalls.incrementAndGet();
			}
			return invocation.proceed();
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
		PolarisSDKContextManager.isRegistered = true;

		// Act
		try {
			manager.createDeregisterAction(registration, proxy, Mockito.mock(BaseInstance.class)).run();
		}
		finally {
			manager.destroy();
			target.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}

		// Assert
		assertThat(deregisterAdviceCalls.get()).isEqualTo(1);
		Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		assertThat(PolarisSDKContextManager.isRegistered).isFalse();
	}

	private static void runLosslessActionThroughSwallowingAdvice(String proxyType) throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		RuntimeException failure = new IllegalStateException("swallowed deregistration failure");
		doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass("cglib".equals(proxyType));
		if ("jdk-restricted".equals(proxyType)) {
			proxyFactory.setInterfaces(ServiceRegistry.class);
		}
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			try {
				return invocation.proceed();
			}
			catch (RuntimeException | Error e) {
				return null;
			}
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, new LosslessProperties());
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		try {
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run())
							.as("proxyType=" + proxyType)
							.isEqualTo(failure);
			Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
			assertThat(PolarisSDKContextManager.isRegistered).as("proxyType=" + proxyType).isTrue();
		}
		finally {
			manager.destroy();
			target.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	private static void runLosslessActionThroughPostDeregisterAdvice(boolean callbackEnabled) throws Exception {
		// Arrange
		TestPolarisServiceRegistry target = createTestPolarisRegistry();
		ProviderAPI providerAPI = target.getProviderAPI();
		PolarisRegistration registration = target.getRegistration();
		RuntimeException failure = new IllegalStateException("post-advice deregistration failure");
		AtomicInteger cleanupCalls = new AtomicInteger();
		AtomicInteger callbackCalls = new AtomicInteger();
		doThrow(failure).when(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if (callbackEnabled) {
			beanFactory.registerSingleton("offlineCallback",
					(LosslessOfflineCallback) context -> callbackCalls.incrementAndGet());
		}
		LosslessProperties properties = new LosslessProperties();
		properties.setOfflineCallbackEnabled(callbackEnabled);
		ProxyFactory proxyFactory = new ProxyFactory(target);
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			Object result = invocation.proceed();
			if ("deregister".equals(invocation.getMethod().getName())) {
				cleanupCalls.incrementAndGet();
			}
			return result;
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties);
		PolarisSDKContextManager.isRegistered = true;

		// Act and assert
		try {
			assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy,
					Mockito.mock(BaseInstance.class)).run())
							.as("callbackEnabled=" + callbackEnabled)
							.isEqualTo(failure);
			assertThat(cleanupCalls.get()).as("callbackEnabled=" + callbackEnabled).isEqualTo(1);
			assertThat(callbackCalls.get()).as("callbackEnabled=" + callbackEnabled)
					.isEqualTo(callbackEnabled ? 1 : 0);
			Mockito.verify(providerAPI).deRegister(Mockito.any(InstanceDeregisterRequest.class));
			assertThat(PolarisSDKContextManager.isRegistered).as("callbackEnabled=" + callbackEnabled).isTrue();
		}
		finally {
			manager.destroy();
			target.destroy();
			PolarisSDKContextManager.isRegistered = false;
		}
	}

	static class TestPolarisServiceRegistry extends PolarisServiceRegistry {

		final ProviderAPI providerAPI;

		final PolarisRegistration registration;

		int legacyCleanupCalls;

		TestPolarisServiceRegistry(PolarisDiscoveryProperties polarisDiscoveryProperties,
				PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryHandler polarisDiscoveryHandler,
				StaticMetadataManager staticMetadataManager, PolarisStatProperties polarisStatProperties,
				ProviderAPI providerAPI, PolarisRegistration registration) {
			super(polarisDiscoveryProperties, polarisSDKContextManager, polarisDiscoveryHandler,
					staticMetadataManager, polarisStatProperties);
			this.providerAPI = providerAPI;
			this.registration = registration;
		}

		ProviderAPI getProviderAPI() {
			return providerAPI;
		}

		PolarisRegistration getRegistration() {
			return registration;
		}

		@Override
		public void deregister(PolarisRegistration registration) {
			super.deregister(registration);
			legacyCleanupCalls++;
		}
	}

	static class SwallowingPolarisServiceRegistry extends TestPolarisServiceRegistry {

		private int swallowCleanupCalls;

		SwallowingPolarisServiceRegistry(PolarisDiscoveryProperties polarisDiscoveryProperties,
				PolarisSDKContextManager polarisSDKContextManager, PolarisDiscoveryHandler polarisDiscoveryHandler,
				StaticMetadataManager staticMetadataManager, PolarisStatProperties polarisStatProperties,
				ProviderAPI providerAPI, PolarisRegistration registration) {
			super(polarisDiscoveryProperties, polarisSDKContextManager, polarisDiscoveryHandler,
					staticMetadataManager, polarisStatProperties, providerAPI, registration);
		}

		@Override
		public void deregister(PolarisRegistration registration) {
			swallowCleanupCalls++;
			try {
				super.deregister(registration);
			}
			catch (RuntimeException e) {
				// Deliberately swallow the failure to model a business override.
			}
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class PolarisPropertiesConfiguration {

	}
}
