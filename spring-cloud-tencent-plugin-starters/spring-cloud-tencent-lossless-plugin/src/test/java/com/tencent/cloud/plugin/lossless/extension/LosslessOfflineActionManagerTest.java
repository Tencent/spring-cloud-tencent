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

package com.tencent.cloud.plugin.lossless.extension;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.cloud.plugin.lossless.LosslessRegistryAspect;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.polaris.api.pojo.BaseInstance;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link LosslessOfflineActionManager}.
 *
 * @author evelynwei
 */
@DisplayName("LosslessOfflineActionManager")
class LosslessOfflineActionManagerTest {

	private final List<LosslessOfflineActionManager> managers = new ArrayList<>();

	@AfterEach
	void tearDown() throws Exception {
		for (LosslessOfflineActionManager manager : managers) {
			manager.destroy();
		}
		managers.clear();
	}

	/**
	 * Verify ordered callbacks receive an immutable context and strict deregistration runs.
	 */
	@DisplayName("test success with ordered callbacks")
	@Test
	void testSuccessWithOrderedCallbacks() {
		// Arrange
		Map<String, String> metadata = new HashMap<>();
		metadata.put("zone", "test-zone");
		TestRegistration registration = new TestRegistration(metadata);
		RecordingRegistry registry = new RecordingRegistry();
		List<String> names = new CopyOnWriteArrayList<>();
		RecordingCallback first = new RecordingCallback("first", 100, context -> names.add("first"));
		RecordingCallback second = new RecordingCallback("second", 200, context -> names.add("second"));
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, first, second);

		// Act
		manager.createDeregisterAction(registration, registry, createInstance()).run();

		// Assert
		assertThat(names).containsExactly("first", "second");
		assertThat(registry.strictCount).isEqualTo(1);
		assertThat(registry.normalCount).isEqualTo(1);
		LosslessOfflineContext context = first.getContexts().get(0);
		assertThat(context.getNamespace()).isEqualTo("test-namespace");
		assertThat(context.getService()).isEqualTo("test-service");
		assertThat(context.getHost()).isEqualTo("127.0.0.1");
		assertThat(context.getPort()).isEqualTo(48080);
		assertThat(context.getMetadata()).isEqualTo(metadata);
		assertThat(Duration.between(context.getStartedAt(), context.getDeadline())).isEqualTo(Duration.ofMillis(5000));
		assertThatThrownBy(() -> context.getMetadata().put("newKey", "newValue"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	/**
	 * Verify the callback snapshot keeps the registered service identity but reads the current port.
	 */
	@DisplayName("test callback context after registration customization")
	@Test
	void testCallbackContextAfterRegistrationCustomization() {
		// Arrange
		TestRegistration registration = new TestRegistration();
		RecordingRegistry registry = new RecordingRegistry();
		List<LosslessOfflineContext> contexts = new CopyOnWriteArrayList<>();
		RecordingCallback callback = new RecordingCallback("customized", 100, contexts::add);
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, callback);
		Runnable action = manager.createDeregisterAction(registration, registry, createInstance());

		// Act
		registration.setServiceId("customized-service");
		registration.setPort(48081);
		action.run();

		// Assert
		assertThat(contexts).hasSize(1);
		assertThat(contexts.get(0).getService()).isEqualTo("test-service");
		assertThat(contexts.get(0).getPort()).isEqualTo(48081);
		assertThat(registry.strictCount).isEqualTo(1);
	}

	/**
	 * Verify every callback is executed while the worker moves from one completed task to the next.
	 */
	@DisplayName("test repeated successful callback chains")
	@Test
	void testRepeatedSuccessfulCallbackChains() {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		AtomicInteger executions = new AtomicInteger();
		RecordingCallback first = new RecordingCallback("first", 100,
				context -> executions.incrementAndGet());
		RecordingCallback second = new RecordingCallback("second", 200,
				context -> executions.incrementAndGet());
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, first, second);
		Runnable action = manager.createDeregisterAction(new TestRegistration(), registry, createInstance());

		// Act
		for (int index = 0; index < 1000; index++) {
			action.run();
		}

		// Assert
		assertThat(executions.get()).isEqualTo(2000);
		assertThat(registry.strictCount).isEqualTo(1000);
	}

	/**
	 * Verify prototype callbacks are found by the existence check and executed by the runner.
	 */
	@DisplayName("test prototype callback")
	@Test
	void testPrototypeCallback() {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		RootBeanDefinition definition = new RootBeanDefinition(RecordingCallback.class);
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		definition.setInstanceSupplier(() -> new RecordingCallback("prototype", 100,
				context -> executions.incrementAndGet()));
		beanFactory.registerBeanDefinition("prototypeCallback", definition);
		LosslessOfflineActionManager manager = createManager(beanFactory);
		RecordingRegistry registry = new RecordingRegistry();

		// Act
		manager.createDeregisterAction(new TestRegistration(), registry, createInstance()).run();

		// Assert
		assertThat(executions.get()).isEqualTo(1);
		assertThat(registry.strictCount).isEqualTo(1);
	}

	/**
	 * Verify callbacks defined in a parent bean factory are executed by the child runner.
	 */
	@DisplayName("test parent bean factory callback")
	@Test
	void testParentBeanFactoryCallback() {
		// Arrange
		DefaultListableBeanFactory parentBeanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		parentBeanFactory.registerSingleton("parentCallback", new RecordingCallback("parent", 100,
				context -> executions.incrementAndGet()));
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(parentBeanFactory);
		LosslessOfflineActionManager manager = createManager(beanFactory);
		RecordingRegistry registry = new RecordingRegistry();

		// Act
		manager.createDeregisterAction(new TestRegistration(), registry, createInstance()).run();

		// Assert
		assertThat(executions.get()).isEqualTo(1);
		assertThat(registry.strictCount).isEqualTo(1);
	}

	/**
	 * Verify continue mode runs the next callback after one callback exits with a failure.
	 */
	@DisplayName("test continue mode after callback failure")
	@Test
	void testContinueModeAfterCallbackFailure() {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		List<String> names = new CopyOnWriteArrayList<>();
		RecordingCallback failed = new RecordingCallback("failed", 100, context -> {
			throw new IllegalStateException("callback failure");
		});
		RecordingCallback next = new RecordingCallback("next", 200, context -> names.add("next"));
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, failed, next);

		// Act and assert
		assertThatCode(() -> manager.createDeregisterAction(new TestRegistration(), registry, createInstance()).run())
				.doesNotThrowAnyException();
		assertThat(names).containsExactly("next");
		assertThat(registry.strictCount).isEqualTo(1);
	}

	/**
	 * Verify abort mode stops before strict deregistration.
	 */
	@DisplayName("test abort mode before deregistration")
	@Test
	void testAbortModeBeforeDeregistration() {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		List<String> names = new CopyOnWriteArrayList<>();
		RecordingCallback failed = new RecordingCallback("failed", 100, context -> {
			throw new IllegalStateException("callback failure");
		});
		RecordingCallback next = new RecordingCallback("next", 200, context -> names.add("next"));
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.ABORT, failed, next);

		// Act and assert
		assertThatThrownBy(() -> manager.createDeregisterAction(new TestRegistration(), registry, createInstance()).run())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CALLBACK_FAILED");
		assertThat(names).isEmpty();
		assertThat(registry.strictCount).isZero();
	}

	/**
	 * Verify a timed out task stays active and the next action skips callbacks but still deregisters.
	 */
	@DisplayName("test timeout keeps active task")
	@Test
	void testTimeoutKeepsActiveTask() {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		AtomicBoolean release = new AtomicBoolean(false);
		AtomicInteger interruptedCount = new AtomicInteger();
		RecordingCallback callback = new RecordingCallback("blocking", 100, context -> {
			while (!release.get()) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
					interruptedCount.incrementAndGet();
				}
			}
		});
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, callback);
		TestRegistration registration = new TestRegistration();
		Runnable firstAction = manager.createDeregisterAction(registration, registry, createInstance());
		Runnable secondAction = manager.createDeregisterAction(registration, registry, createInstance());

		// Act
		try {
			assertThatCode(firstAction::run).doesNotThrowAnyException();
			assertThat(registry.strictCount).isEqualTo(1);
			assertThatCode(secondAction::run).doesNotThrowAnyException();
			assertThat(callback.getExecutions()).isEqualTo(1);
			assertThat(registry.strictCount).isEqualTo(2);
		}
		finally {
			release.set(true);
		}

		// Assert
		assertThat(interruptedCount.get()).isGreaterThanOrEqualTo(1);
	}

	/**
	 * Verify disabled callbacks do not run but strict deregistration remains enabled.
	 */
	@DisplayName("test disabled callback")
	@Test
	void testDisabledCallback() {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		RecordingCallback callback = new RecordingCallback("disabled", 100, context -> {
			throw new IllegalStateException("callback should not run");
		});
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, false, callback);

		// Act and assert
		assertThatCode(() -> manager.createDeregisterAction(new TestRegistration(), registry, createInstance()).run())
				.doesNotThrowAnyException();
		assertThat(callback.getExecutions()).isZero();
		assertThat(registry.strictCount).isEqualTo(1);
	}

	/**
	 * Verify a non-Polaris registry keeps its original deregistration contract.
	 */
	@DisplayName("test non polaris registry keeps original deregistration")
	@Test
	void testNonPolarisRegistryKeepsOriginalDeregistration() {
		// Arrange
		NonStrictRegistry registry = new NonStrictRegistry();
		RecordingCallback callback = new RecordingCallback("callback", 100, context -> {
		});
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, callback);

		// Act and assert
		assertThatCode(() -> manager.createDeregisterAction(new TestRegistration(), registry, new TestInstance(),
				null, false).run()).doesNotThrowAnyException();
		assertThat(callback.getExecutions()).isEqualTo(1);
		assertThat(registry.normalCount).isEqualTo(1);
	}

	/**
	 * Verify an interrupted caller with an active task does not continue strict deregistration.
	 */
	@DisplayName("test interrupted caller with active task")
	@Test
	void testInterruptedCallerWithActiveTask() throws Exception {
		// Arrange
		RecordingRegistry registry = new RecordingRegistry();
		CountDownLatch release = new CountDownLatch(1);
		AtomicInteger executions = new AtomicInteger();
		RecordingCallback callback = new RecordingCallback("blocking", 100, context -> {
			executions.incrementAndGet();
			while (true) {
				try {
					if (release.await(50, TimeUnit.MILLISECONDS)) {
						break;
					}
				}
				catch (InterruptedException e) {
					// Keep the task active even after cancellation interrupts the worker.
				}
			}
		});
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE, callback);
		TestRegistration registration = new TestRegistration();
		Runnable firstAction = manager.createDeregisterAction(registration, registry, createInstance());
		Runnable secondAction = manager.createDeregisterAction(registration, registry, createInstance());
		AtomicReference<Throwable> secondFailure = new AtomicReference<>();
		Thread caller = new Thread(() -> {
			Thread.currentThread().interrupt();
			try {
				secondAction.run();
			}
			catch (Throwable failure) {
				secondFailure.set(failure);
			}
		}, "test-lossless-interrupted-caller");

		// Act
		try {
			assertThatCode(firstAction::run).doesNotThrowAnyException();
			caller.start();
			caller.join(2000);

			// Assert
			assertThat(caller.isAlive()).isFalse();
			assertThat(secondFailure.get()).isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("CALLER_INTERRUPTED");
			assertThat(executions.get()).isEqualTo(1);
			assertThat(registry.strictCount).isEqualTo(1);
		}
		finally {
			release.countDown();
		}
	}

	/**
	 * Verify a restricted JDK proxy still propagates strict deregistration failures.
	 */
	@DisplayName("test restricted jdk proxy strict deregistration")
	@Test
	void testRestrictedJdkProxyStrictDeregistration() {
		// Arrange
		StrictRegistry target = new StrictRegistry();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(target);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

		// Act and assert
		assertThatThrownBy(() -> manager.createDeregisterAction(new TestRegistration(), proxy, createInstance()).run())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("strict deregistration failed");
		assertThat(target.strictCount).isEqualTo(1);
		assertThat(target.normalCount).isEqualTo(1);
	}

	/**
	 * Verify an opaque proxy still propagates the strict deregistration failure.
	 */
	@DisplayName("test opaque restricted jdk proxy does not silently degrade")
	@Test
	void testOpaqueRestrictedJdkProxyDoesNotSilentlyDegrade() {
		// Arrange
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

		// Act and assert
		StrictRegistry target = new StrictRegistry();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(target);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.setOpaque(true);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();

		assertThatThrownBy(
				() -> manager.createDeregisterAction(new TestRegistration(), proxy, new TestInstance()).run())
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("strict deregistration failed");
		assertThat(target.strictCount).isEqualTo(1);
		assertThat(target.normalCount).isEqualTo(1);
	}

	/**
	 * Verify a strict target bound at registration keeps the original proxy deregistration path.
	 */
	@DisplayName("test bound strict target with unwrappable restricted jdk proxy")
	@Test
	void testBoundStrictTargetWithUnwrappableRestrictedJdkProxy() {
		// Arrange
		for (String proxyType : List.of("opaque", "hot-swappable")) {
			for (boolean callbackEnabled : new boolean[] { false, true }) {
				StrictRegistry target = new StrictRegistry();
				ProxyFactory proxyFactory = new ProxyFactory();
				if ("opaque".equals(proxyType)) {
					proxyFactory.setTarget(target);
				}
				else {
					proxyFactory.setTargetSource(new HotSwappableTargetSource(target));
				}
				proxyFactory.setInterfaces(ServiceRegistry.class);
				proxyFactory.setProxyTargetClass(false);
				proxyFactory.setOpaque("opaque".equals(proxyType));
				@SuppressWarnings("unchecked")
				ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
				RecordingCallback callback = new RecordingCallback("callback", 100, context -> {
				});
				LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE,
						callbackEnabled, callback);

				// Act and assert
				assertThatThrownBy(() -> manager.createDeregisterAction(new TestRegistration(), proxy,
						new TestInstance(), target).run())
								.as(proxyType + " callbackEnabled=" + callbackEnabled)
								.isInstanceOf(IllegalStateException.class)
								.hasMessage("strict deregistration failed");
				assertThat(target.strictCount).as(proxyType).isEqualTo(1);
				assertThat(target.normalCount).as(proxyType).isEqualTo(1);
				assertThat(callback.getExecutions()).as("callbackEnabled=" + callbackEnabled)
						.isEqualTo(callbackEnabled ? 1 : 0);
			}
		}
	}

	/**
	 * Verify an opaque CGLIB proxy deregisters through its real target without a false target change.
	 */
	@DisplayName("test opaque cglib strict deregistration")
	@Test
	void testOpaqueCglibStrictDeregistration() {
		// Arrange
		TestRegistration registration = new TestRegistration();
		CglibStrictRegistry target = new CglibStrictRegistry();
		LosslessRegistryAspect aspect = new LosslessRegistryAspect(null, registration, null,
				new LosslessProperties(), null, null, null, true);
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.setOpaque(true);
		proxyFactory.addAspect(aspect);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		setServiceRegistry(aspect, proxy);
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

		// Act and assert
		assertThatCode(() -> manager.createDeregisterAction(registration, proxy, new TestInstance(),
				target, true).run()).doesNotThrowAnyException();
		assertThat(target.strictCount).isEqualTo(1);
		assertThat(target.normalCount).isEqualTo(1);
	}

	/**
	 * Verify an opaque JDK HotSwap does not reuse the strict target after it is replaced.
	 */
	@DisplayName("test opaque hot swap without strict support after replacement")
	@Test
	void testOpaqueHotSwapWithoutStrictSupportAfterReplacement() {
		// Arrange
		StrictRegistry strictTarget = new StrictRegistry();
		NonStrictRegistry nonStrictTarget = new NonStrictRegistry();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(strictTarget);
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.setOpaque(true);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);
		Runnable action = manager.createDeregisterAction(new TestRegistration(), proxy, new TestInstance(),
				strictTarget);

		// Act
		targetSource.swap(nonStrictTarget);

		// Assert
		assertThatThrownBy(action::run)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Lossless deregistration is not available for the current hot-swappable target.");
		assertThat(strictTarget.strictCount).isZero();
		assertThat(strictTarget.normalCount).isZero();
		assertThat(nonStrictTarget.normalCount).isZero();
	}

	/**
	 * Verify a transparent hot-swappable proxy uses its current target for strict deregistration.
	 */
	@DisplayName("test hot swappable target changes after action creation")
	@Test
	void testHotSwappableTargetChangesAfterActionCreation() {
		// Arrange
		StrictRegistry firstTarget = new StrictRegistry();
		StrictRegistry secondTarget = new StrictRegistry();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(firstTarget);
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);
		Runnable action = manager.createDeregisterAction(new TestRegistration(), proxy, new TestInstance(),
				firstTarget);

		// Act
		targetSource.swap(secondTarget);
		assertThatThrownBy(action::run)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("strict deregistration failed");

		// Assert
		assertThat(firstTarget.strictCount).isZero();
		assertThat(secondTarget.strictCount).isEqualTo(1);
		assertThat(secondTarget.normalCount).isEqualTo(1);
	}

	/**
	 * Verify a transparent hot-swappable proxy does not reuse a stale strict target after a swap.
	 */
	@DisplayName("test hot swappable target without strict support after swap")
	@Test
	void testHotSwappableTargetWithoutStrictSupportAfterSwap() {
		// Arrange
		StrictRegistry strictTarget = new StrictRegistry();
		NonStrictRegistry nonStrictTarget = new NonStrictRegistry();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(strictTarget);
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);
		Runnable action = manager.createDeregisterAction(new TestRegistration(), proxy, new TestInstance(),
				strictTarget);

		// Act
		targetSource.swap(nonStrictTarget);
		assertThatThrownBy(action::run)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Lossless deregistration is not available for the current hot-swappable target.");

		// Assert
		assertThat(strictTarget.strictCount).isZero();
		assertThat(strictTarget.normalCount).isZero();
		assertThat(nonStrictTarget.normalCount).isZero();
	}

	/**
	 * Verify a hot-swappable target behind a singleton proxy is resolved at execution time.
	 */
	@DisplayName("test nested hot swappable target changes after action creation")
	@Test
	void testNestedHotSwappableTargetChangesAfterActionCreation() {
		// Arrange
		StrictRegistry firstTarget = new StrictRegistry();
		StrictRegistry secondTarget = new StrictRegistry();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(firstTarget);
		ProxyFactory innerProxyFactory = new ProxyFactory();
		innerProxyFactory.setTargetSource(targetSource);
		innerProxyFactory.setInterfaces(ServiceRegistry.class);
		innerProxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> innerProxy = (ServiceRegistry<Registration>) innerProxyFactory.getProxy();
		ProxyFactory outerProxyFactory = new ProxyFactory();
		outerProxyFactory.setTarget(innerProxy);
		outerProxyFactory.setInterfaces(ServiceRegistry.class);
		outerProxyFactory.setProxyTargetClass(false);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> outerProxy = (ServiceRegistry<Registration>) outerProxyFactory.getProxy();
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);
		Runnable action = manager.createDeregisterAction(new TestRegistration(), outerProxy, new TestInstance(),
				firstTarget);

		// Act
		targetSource.swap(secondTarget);
		assertThatThrownBy(action::run)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("strict deregistration failed");

		// Assert
		assertThat(firstTarget.strictCount).isZero();
		assertThat(firstTarget.normalCount).isZero();
		assertThat(secondTarget.strictCount).isEqualTo(1);
		assertThat(secondTarget.normalCount).isEqualTo(1);
	}

	/**
	 * Verify a target swap during strict deregistration fails instead of reporting success.
	 */
	@DisplayName("test hot swap during deregistration")
	@Test
	void testHotSwapDuringDeregistration() {
		for (boolean exposeStrictInterface : new boolean[] { false, true }) {
			// Arrange
			RecordingRegistry firstTarget = new RecordingRegistry();
			RecordingRegistry secondTarget = new RecordingRegistry();
			HotSwappableTargetSource targetSource = new HotSwappableTargetSource(firstTarget);
			ProxyFactory proxyFactory = new ProxyFactory();
			proxyFactory.setTargetSource(targetSource);
			if (exposeStrictInterface) {
				proxyFactory.setInterfaces(ServiceRegistry.class, LosslessDeregistration.class);
			}
			else {
				proxyFactory.setInterfaces(ServiceRegistry.class);
			}
			proxyFactory.setProxyTargetClass(false);
			AtomicBoolean swapped = new AtomicBoolean(false);
			AtomicReference<ServiceRegistry<Registration>> proxyReference = new AtomicReference<>();
			proxyFactory.addAdvice((MethodInterceptor) invocation -> {
				if ("deregister".equals(invocation.getMethod().getName()) && swapped.compareAndSet(false, true)) {
					targetSource.swap(secondTarget);
					Object registrationArgument = invocation.getArguments()[0];
					proxyReference.get().deregister((Registration) registrationArgument);
					return null;
				}
				return invocation.proceed();
			});
			@SuppressWarnings("unchecked")
			ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
			proxyReference.set(proxy);
			LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

			// Act and assert
			assertThatThrownBy(() -> manager.createDeregisterAction(new TestRegistration(), proxy,
					createInstance(), null, true).run())
							.as("exposeStrictInterface=" + exposeStrictInterface)
							.isInstanceOf(IllegalStateException.class)
							.hasMessage("Lossless deregistration target changed during the action.");
			assertThat(firstTarget.strictCount).as("exposeStrictInterface=" + exposeStrictInterface).isEqualTo(1);
			assertThat(firstTarget.normalCount).as("exposeStrictInterface=" + exposeStrictInterface).isZero();
			assertThat(secondTarget.strictCount).as("exposeStrictInterface=" + exposeStrictInterface).isZero();
			assertThat(secondTarget.normalCount).as("exposeStrictInterface=" + exposeStrictInterface).isEqualTo(1);
		}
	}

	/**
	 * Verify a nested proxy invocation with the same registration cannot bypass the target guard.
	 */
	@DisplayName("test nested proxy target swap and restore")
	@Test
	void testNestedProxyTargetSwapAndRestore() {
		// Arrange
		RecordingRegistry firstTarget = new RecordingRegistry();
		RecordingRegistry secondTarget = new RecordingRegistry();
		TestRegistration registration = new TestRegistration();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(firstTarget);
		LosslessRegistryAspect aspect = new LosslessRegistryAspect(null, registration, null,
				new LosslessProperties(), null, null, null, true);
		AspectJProxyFactory innerProxyFactory = new AspectJProxyFactory();
		innerProxyFactory.setTargetSource(targetSource);
		innerProxyFactory.setInterfaces(ServiceRegistry.class);
		innerProxyFactory.setProxyTargetClass(false);
		innerProxyFactory.addAspect(aspect);
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> innerProxy = (ServiceRegistry<Registration>) innerProxyFactory.getProxy();
		ProxyFactory outerProxyFactory = new ProxyFactory();
		outerProxyFactory.setTarget(innerProxy);
		outerProxyFactory.setInterfaces(ServiceRegistry.class);
		outerProxyFactory.setProxyTargetClass(false);
		AtomicBoolean swapped = new AtomicBoolean(false);
		outerProxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if ("deregister".equals(invocation.getMethod().getName()) && swapped.compareAndSet(false, true)) {
				targetSource.swap(secondTarget);
				try {
					innerProxy.deregister((Registration) invocation.getArguments()[0]);
					return null;
				}
				finally {
					targetSource.swap(firstTarget);
				}
			}
			return invocation.proceed();
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> outerProxy = (ServiceRegistry<Registration>) outerProxyFactory.getProxy();
		setServiceRegistry(aspect, outerProxy);
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

		// Act and assert
		assertThatThrownBy(() -> manager.createDeregisterAction(registration, outerProxy, createInstance(),
				firstTarget, true).run())
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Lossless deregistration target changed during the action.");
		assertThat(firstTarget.strictCount).isEqualTo(1);
		assertThat(firstTarget.normalCount).isZero();
		assertThat(secondTarget.strictCount).isZero();
		assertThat(secondTarget.normalCount).isZero();
	}

	/**
	 * Verify a target that is swapped and restored around the actual invocation cannot bypass strict checks.
	 */
	@DisplayName("test target swap and restore during deregistration")
	@Test
	void testTargetSwapAndRestoreDuringDeregistration() {
		// Arrange
		RecordingRegistry firstTarget = new RecordingRegistry();
		RecordingRegistry secondTarget = new RecordingRegistry();
		HotSwappableTargetSource targetSource = new HotSwappableTargetSource(firstTarget);
		TestRegistration registration = new TestRegistration();
		AspectJProxyFactory proxyFactory = new AspectJProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setInterfaces(ServiceRegistry.class);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.addAspect(new LosslessRegistryAspect(null, registration, null, new LosslessProperties(),
				null, null, null, true));
		AtomicBoolean swapped = new AtomicBoolean(false);
		AtomicReference<ServiceRegistry<Registration>> proxyReference = new AtomicReference<>();
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if ("deregister".equals(invocation.getMethod().getName()) && swapped.compareAndSet(false, true)) {
				targetSource.swap(secondTarget);
				try {
					proxyReference.get().deregister((Registration) invocation.getArguments()[0]);
					return null;
				}
				finally {
					targetSource.swap(firstTarget);
				}
			}
			return invocation.proceed();
		});
		@SuppressWarnings("unchecked")
		ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();
		proxyReference.set(proxy);
		LosslessOfflineActionManager manager = createManager(OfflineCallbackFailureMode.CONTINUE);

		// Act and assert
		assertThatThrownBy(() -> manager.createDeregisterAction(registration, proxy, createInstance(),
				null, true).run())
						.isInstanceOf(IllegalStateException.class)
						.hasMessage("Lossless deregistration target changed during the action.");
		assertThat(firstTarget.strictCount).isEqualTo(1);
		assertThat(firstTarget.normalCount).isZero();
		assertThat(secondTarget.strictCount).isZero();
		assertThat(secondTarget.normalCount).isZero();
	}

	private LosslessOfflineActionManager createManager(OfflineCallbackFailureMode failureMode,
			RecordingCallback... callbacks) {
		return createManager(failureMode, true, callbacks);
	}

	private LosslessOfflineActionManager createManager(OfflineCallbackFailureMode failureMode, boolean enabled,
			RecordingCallback... callbacks) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		for (RecordingCallback callback : callbacks) {
			beanFactory.registerSingleton(callback.getName(), callback);
		}
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory,
				managerProperties(failureMode, enabled));
		managers.add(manager);
		return manager;
	}

	private LosslessOfflineActionManager createManager(DefaultListableBeanFactory beanFactory) {
		LosslessOfflineActionManager manager = new LosslessOfflineActionManager(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory,
				managerProperties(OfflineCallbackFailureMode.CONTINUE, true));
		managers.add(manager);
		return manager;
	}

	private static void setServiceRegistry(LosslessRegistryAspect aspect,
			ServiceRegistry<Registration> serviceRegistry) {
		try {
			Field field = LosslessRegistryAspect.class.getDeclaredField("serviceRegistry");
			field.setAccessible(true);
			field.set(aspect, serviceRegistry);
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private LosslessProperties managerProperties(OfflineCallbackFailureMode failureMode, boolean enabled) {
		LosslessProperties properties = new LosslessProperties();
		properties.setOfflineCallbackTimeout(1000);
		properties.setOfflineCallbackTotalTimeout(5000);
		properties.setOfflineCallbackFailureMode(failureMode);
		properties.setOfflineCallbackEnabled(enabled);
		return properties;
	}

	private BaseInstance createInstance() {
		BaseInstance instance = Mockito.mock(BaseInstance.class);
		Mockito.when(instance.getNamespace()).thenReturn("test-namespace");
		Mockito.when(instance.getService()).thenReturn("test-service");
		Mockito.when(instance.getHost()).thenReturn("127.0.0.1");
		Mockito.when(instance.getPort()).thenReturn(48080);
		return instance;
	}

	private static final class TestInstance implements BaseInstance {

		@Override
		public String getNamespace() {
			return "test-namespace";
		}

		@Override
		public String getService() {
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
	}

	private interface CallbackAction {

		void execute(LosslessOfflineContext context) throws Exception;
	}

	private static final class RecordingCallback implements LosslessOfflineCallback, Ordered {

		private final String name;

		private final int order;

		private final CallbackAction action;

		private final AtomicInteger executions = new AtomicInteger();

		private final List<LosslessOfflineContext> contexts = new CopyOnWriteArrayList<>();

		private RecordingCallback(String name, int order, CallbackAction action) {
			this.name = name;
			this.order = order;
			this.action = action;
		}

		private String getName() {
			return name;
		}

		private int getExecutions() {
			return executions.get();
		}

		private List<LosslessOfflineContext> getContexts() {
			return contexts;
		}

		@Override
		public void onOffline(LosslessOfflineContext context) throws Exception {
			executions.incrementAndGet();
			contexts.add(context);
			action.execute(context);
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

	private static final class TestRegistration implements Registration {

		private final Map<String, String> metadata;

		private String serviceId;

		private int port;

		private TestRegistration() {
			this(new HashMap<>());
		}

		private TestRegistration(Map<String, String> metadata) {
			this.metadata = metadata;
			this.serviceId = "test-service";
			this.port = 48080;
		}

		@Override
		public String getServiceId() {
			return serviceId;
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return port;
		}

		private void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

		private void setPort(int port) {
			this.port = port;
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
			return metadata;
		}

		@Override
		public String getInstanceId() {
			return "test-instance";
		}
	}

	private static final class RecordingRegistry implements ServiceRegistry<Registration>, LosslessDeregistration {

		private int strictCount;

		private int normalCount;

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
			normalCount++;
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
			strictCount++;
			deregistration.run();
			return LosslessDeregistrationResult.success();
		}
	}

	private static final class StrictRegistry implements ServiceRegistry<Registration>, LosslessDeregistration {

		private int strictCount;

		private int normalCount;

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
			normalCount++;
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
			strictCount++;
			deregistration.run();
			return LosslessDeregistrationResult.failed(new IllegalStateException("strict deregistration failed"));
		}
	}

	private static final class NonStrictRegistry implements ServiceRegistry<Registration> {

		private int normalCount;

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
			normalCount++;
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

	public static class CglibStrictRegistry implements ServiceRegistry<Registration>, LosslessDeregistration {

		private int strictCount;

		private int normalCount;

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
			normalCount++;
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
			strictCount++;
			deregistration.run();
			return LosslessDeregistrationResult.success();
		}
	}
}
