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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link LosslessOfflineCallbackRunner}.
 *
 * @author evelynwei
 */
@DisplayName("LosslessOfflineCallbackRunner")
class LosslessOfflineCallbackRunnerTest {

	private static final String CALLBACK_STARTED_MESSAGE = "Lossless offline callback [{}] started.";

	private static final long MAX_TIMEOUT_MILLIS = Long.MAX_VALUE / 1_000_000L;

	private static final CountDownLatch GATED_THREAD_PUBLICATION = new CountDownLatch(1);

	private static final CountDownLatch RELEASE_THREAD_PUBLICATION = new CountDownLatch(1);

	/**
	 * Verify callback resolution does not create a prototype callback a second time for logging.
	 */
	@DisplayName("test prototype callback is resolved once")
	@Test
	void testPrototypeCallbackIsResolvedOnce() {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger constructions = new AtomicInteger();
		AtomicInteger executions = new AtomicInteger();
		RootBeanDefinition definition = new RootBeanDefinition(LosslessOfflineCallback.class);
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		definition.setInstanceSupplier(() -> {
			if (!constructions.compareAndSet(0, 1)) {
				throw new IllegalStateException("prototype callback was created twice");
			}
			return (LosslessOfflineCallback) context -> executions.incrementAndGet();
		});
		beanFactory.registerBeanDefinition("prototypeCallback", definition);
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties());

		// Act
		LosslessOfflineCallbackResult result = runner.execute(context());

		// Assert
		assertThat(result.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
		assertThat(constructions.get()).isEqualTo(1);
		assertThat(executions.get()).isEqualTo(1);
	}

	/**
	 * Verify a non-autowire-candidate callback is not created while resolving valid callbacks.
	 */
	@DisplayName("test excluded callback is not resolved")
	@Test
	void testExcludedCallbackIsNotResolved() {
		// Arrange
		AtomicInteger excludedConstructions = new AtomicInteger();
		AtomicInteger executions = new AtomicInteger();
		try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
			RootBeanDefinition definition = new RootBeanDefinition(LosslessOfflineCallback.class);
			definition.setLazyInit(true);
			definition.setAutowireCandidate(false);
			definition.setInstanceSupplier(() -> {
				excludedConstructions.incrementAndGet();
				throw new IllegalStateException("excluded callback must not be created");
			});
			applicationContext.registerBeanDefinition("excludedCallback", definition);
			applicationContext.registerBean("validCallback", LosslessOfflineCallback.class,
					() -> callbackContext -> executions.incrementAndGet());
			applicationContext.register(RunnerConfiguration.class);
			applicationContext.refresh();
			LosslessOfflineCallbackRunner runner = applicationContext.getBean(LosslessOfflineCallbackRunner.class);

			// Act
			LosslessOfflineCallbackResult result = runner.execute(context());

			// Assert
			assertThat(result.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
			assertThat(excludedConstructions.get()).isZero();
			assertThat(executions.get()).isEqualTo(1);
		}
	}

	/**
	 * Verify the largest configurable timeout still permits a fast callback.
	 */
	@DisplayName("test maximum total timeout executes fast callback")
	@Test
	void testMaximumTotalTimeoutExecutesFastCallback() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		beanFactory.registerSingleton("callback",
				(LosslessOfflineCallback) context -> executions.incrementAndGet());
		LosslessProperties properties = properties();
		properties.setOfflineCallbackTotalTimeout(MAX_TIMEOUT_MILLIS);
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties);

		// Act
		try {
			LosslessOfflineCallbackResult result = runner.execute(context());

			// Assert
			assertThat(result.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
			assertThat(executions.get()).isEqualTo(1);
		}
		finally {
			runner.destroy();
		}
	}

	/**
	 * Verify a failed executor factory does not leave an active task behind.
	 */
	@DisplayName("test executor factory failure releases active task")
	@Test
	void testExecutorFactoryFailureReleasesActiveTask() throws InterruptedException {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> {
		});
		AtomicBoolean failNextFactory = new AtomicBoolean(true);
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties(), () -> {
					if (failNextFactory.getAndSet(false)) {
						throw new IllegalStateException("factory failure");
					}
					return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
							new LinkedBlockingQueue<>(1), LosslessOfflineCallbackRunnerTest::createThread);
				});

		// Act and assert
		try {
			LosslessOfflineCallbackResult firstResult = runner.execute(context());
			LosslessOfflineCallbackResult secondResult = runner.execute(context());
			assertThat(firstResult.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUBMIT_REJECTED);
			assertThat(secondResult.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
		}
		finally {
			runner.destroy();
		}
	}

	/**
	 * Verify destroy waits for lazy executor creation and closes the created executor.
	 */
	@DisplayName("test destroy during lazy executor creation")
	@Test
	void testDestroyDuringLazyExecutorCreation() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> {
		});
		CountDownLatch factoryStarted = new CountDownLatch(1);
		CountDownLatch releaseFactory = new CountDownLatch(1);
		AtomicReference<ExecutorService> createdExecutor = new AtomicReference<>();
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties(), () -> {
					factoryStarted.countDown();
					boolean released;
					try {
						released = releaseFactory.await(2, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						released = false;
					}
					assertThat(released).isTrue();
					ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
							new LinkedBlockingQueue<>(1), LosslessOfflineCallbackRunnerTest::createThread);
					createdExecutor.set(executor);
					return executor;
				});
		AtomicReference<LosslessOfflineCallbackResult> result = new AtomicReference<>();
		Thread executionThread = new Thread(() -> result.set(runner.execute(context())), "test-lossless-execute");

		// Act
		executionThread.start();
		assertThat(factoryStarted.await(2, TimeUnit.SECONDS)).isTrue();
		Thread destroyThread = new Thread(() -> {
			try {
				runner.destroy();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "test-lossless-destroy");
		destroyThread.start();
		Thread.sleep(100);
		releaseFactory.countDown();
		executionThread.join(2000);
		destroyThread.join(2000);

		// Assert
		assertThat(executionThread.isAlive()).isFalse();
		assertThat(destroyThread.isAlive()).isFalse();
		assertThat(result.get()).isNotNull();
		assertThat(createdExecutor.get()).isNotNull();
		assertThat(createdExecutor.get().isShutdown()).isTrue();
	}

	/**
	 * Verify cancelling a queued task releases the bounded queue slot for the next action.
	 */
	@DisplayName("test cancelled queued task releases queue capacity")
	@Test
	void testCancelledQueuedTaskReleasesQueueCapacity() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> executions.incrementAndGet());
		BlockingAfterExecuteExecutor executor = new BlockingAfterExecuteExecutor();
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties(), () -> executor);

		// Act and assert
		try {
			assertThat(runner.execute(context()).getStatus())
					.isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
			assertThat(executor.awaitWorkerBlocked()).isTrue();
			LosslessOfflineCallbackResult queuedResult = runner.execute(context());
			assertThat(queuedResult.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
			assertThat(executor.getQueue()).isEmpty();
			LosslessOfflineCallbackResult retryResult = runner.execute(context());
			assertThat(retryResult.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
			assertThat(executions.get()).isEqualTo(1);
		}
		finally {
			executor.releaseWorker();
			runner.destroy();
		}
	}

	/**
	 * Verify cancellation during task startup does not enter the callback without an interrupt.
	 */
	@DisplayName("test cancellation during task startup")
	@Test
	void testCancellationDuringTaskStartup() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		AtomicBoolean interrupted = new AtomicBoolean(false);
		AtomicBoolean blockCallback = new AtomicBoolean(false);
		CountDownLatch callbackRelease = new CountDownLatch(1);
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> {
			executions.incrementAndGet();
			if (blockCallback.get()) {
				try {
					callbackRelease.await();
				}
				catch (InterruptedException e) {
					interrupted.set(true);
					throw e;
				}
			}
		});
		CapturingExecutor executor = new CapturingExecutor();
		LosslessProperties startupProperties = timeoutProperties();
		startupProperties.setOfflineCallbackTimeout(1000);
		startupProperties.setOfflineCallbackTotalTimeout(5000);
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, startupProperties,
				() -> executor);
		AtomicReference<LosslessOfflineCallbackResult> result = new AtomicReference<>();
		Thread caller = new Thread(() -> result.set(runner.execute(context())), "test-lossless-caller");

		// Act
		caller.start();
		caller.join(2000);
		assertThat(result.get().getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
		blockCallback.set(true);
		executions.set(0);
		executor.startCapture();
		result.set(null);
		caller = new Thread(() -> result.set(runner.execute(context())), "test-lossless-caller");
		caller.start();
		assertThat(executor.awaitTaskCaptured()).isTrue();
		gateThreadPublication(executor.getTask());
		Thread worker = new Thread(executor.getTask()::run, "test-lossless-worker");
		worker.start();
		assertThat(executor.awaitThreadPublication()).isTrue();
		Thread.sleep(50);
		assertThat(caller.isAlive()).isTrue();
		caller.join(2000);
		executor.releaseThreadPublication();
		worker.join(2000);

		// Assert
		assertThat(caller.isAlive()).isFalse();
		assertThat(worker.isAlive()).isFalse();
		assertThat(result.get().getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
		assertThat(executions.get()).isZero();
		assertThat(interrupted.get()).isFalse();

		try {
			runner.destroy();
		}
		finally {
			callbackRelease.countDown();
		}
	}

	/**
	 * Verify a callback is not submitted after logging blocks beyond the total deadline.
	 */
	@DisplayName("test callback is not submitted after total deadline")
	@Test
	void testCallbackIsNotSubmittedAfterTotalDeadline() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> executions.incrementAndGet());
		LosslessProperties properties = timeoutProperties();
		properties.setOfflineCallbackTimeout(1000);
		properties.setOfflineCallbackTotalTimeout(50);
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties);
		CountDownLatch logReached = new CountDownLatch(1);
		CountDownLatch releaseLog = new CountDownLatch(1);
		UnsynchronizedAppenderBase<ILoggingEvent> appender = new UnsynchronizedAppenderBase<>() {
			@Override
			protected void append(ILoggingEvent event) {
				if (CALLBACK_STARTED_MESSAGE.equals(event.getMessage())) {
					logReached.countDown();
					await(releaseLog);
				}
			}
		};
		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(LosslessOfflineCallbackRunner.class);
		Level originalLevel = logger.getLevel();
		boolean originalAdditive = logger.isAdditive();
		logger.setLevel(Level.INFO);
		logger.setAdditive(false);
		appender.start();
		logger.addAppender(appender);
		AtomicReference<LosslessOfflineCallbackResult> result = new AtomicReference<>();
		Thread caller = new Thread(() -> result.set(runner.execute(context())), "test-lossless-deadline");

		// Act
		try {
			caller.start();
			assertThat(logReached.await(2, TimeUnit.SECONDS)).isTrue();
			Thread.sleep(100);
			releaseLog.countDown();
			caller.join(2000);

			// Assert
			assertThat(caller.isAlive()).isFalse();
			assertThat(result.get().getStatus())
					.isEqualTo(LosslessOfflineCallbackResult.Status.BUDGET_EXHAUSTED);
			assertThat(executions.get()).isZero();
		}
		finally {
			releaseLog.countDown();
			caller.join(2000);
			logger.detachAppender(appender);
			appender.stop();
			logger.setLevel(originalLevel);
			logger.setAdditive(originalAdditive);
			runner.destroy();
		}
	}

	/**
	 * Verify a callback that finishes after its single timeout is not reported as successful.
	 */
	@DisplayName("test callback completed after single timeout before wait")
	@Test
	void testCallbackCompletedAfterSingleTimeoutBeforeWait() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> {
			executions.incrementAndGet();
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		LosslessProperties properties = timeoutProperties();
		properties.setOfflineCallbackTimeout(20);
		properties.setOfflineCallbackTotalTimeout(2000);
		CompletedBeforeWaitExecutor executor = new CompletedBeforeWaitExecutor();
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties, () -> executor);

		// Act
		try {
			LosslessOfflineCallbackResult result = runner.execute(context());

			// Assert
			assertThat(result.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
			assertThat(executions.get()).isEqualTo(1);
		}
		finally {
			runner.destroy();
		}
	}

	/**
	 * Verify an exceptionally completed callback is still classified as a timeout.
	 */
	@DisplayName("test exceptional callback completion after single timeout")
	@Test
	void testExceptionalCallbackCompletionAfterSingleTimeout() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger firstExecutions = new AtomicInteger();
		AtomicInteger secondExecutions = new AtomicInteger();
		IllegalStateException callbackFailure = new IllegalStateException("callback failure");
		beanFactory.registerSingleton("first", (LosslessOfflineCallback) context -> {
			firstExecutions.incrementAndGet();
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			throw callbackFailure;
		});
		beanFactory.registerSingleton("second", (LosslessOfflineCallback) context -> secondExecutions.incrementAndGet());
		LosslessProperties properties = timeoutProperties();
		properties.setOfflineCallbackTimeout(20);
		properties.setOfflineCallbackTotalTimeout(2000);
		properties.setOfflineCallbackFailureMode(OfflineCallbackFailureMode.CONTINUE);
		ExceptionalAfterDeadlineExecutor executor = new ExceptionalAfterDeadlineExecutor();
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties, () -> executor);

		// Act
		try {
			LosslessOfflineCallbackResult result = runner.execute(context());

			// Assert
			assertThat(result.getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
			assertThat(result.getFailure()).isSameAs(callbackFailure);
			assertThat(result.isFailAction()).isFalse();
			assertThat(firstExecutions.get()).isEqualTo(1);
			assertThat(secondExecutions.get()).isZero();
		}
		finally {
			runner.destroy();
		}
	}

	/**
	 * Verify a queued callback is not started after its deadline while the worker was delayed.
	 */
	@DisplayName("test queued callback is not started after deadline")
	@Test
	void testQueuedCallbackIsNotStartedAfterDeadline() throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AtomicInteger executions = new AtomicInteger();
		beanFactory.registerSingleton("callback", (LosslessOfflineCallback) context -> executions.incrementAndGet());
		LosslessProperties properties = timeoutProperties();
		properties.setOfflineCallbackTimeout(20);
		properties.setOfflineCallbackTotalTimeout(200);
		LateWorkerStartExecutor executor = new LateWorkerStartExecutor();
		LosslessOfflineCallbackRunner runner = new LosslessOfflineCallbackRunner(
				beanFactory.getBeanProvider(LosslessOfflineCallback.class), beanFactory, properties, () -> executor);
		AtomicReference<LosslessOfflineCallbackResult> result = new AtomicReference<>();

		// Act
		try {
			assertThat(runner.execute(context()).getStatus())
					.isEqualTo(LosslessOfflineCallbackResult.Status.SUCCESS);
			assertThat(executions.get()).isEqualTo(1);
			executions.set(0);
			assertThat(executor.awaitFirstWorkerBlocked()).isTrue();
			Thread caller = new Thread(() -> result.set(runner.execute(context())), "test-lossless-late-worker");
			caller.start();
			assertThat(executor.awaitSecondSubmissionBlocked()).isTrue();
			Thread.sleep(100);
			executor.releaseFirstWorker();
			assertThat(executor.awaitSecondWorkerCompleted()).isTrue();
			Thread.sleep(50);
			executor.releaseSecondSubmission();
			caller.join(2000);

			// Assert
			assertThat(caller.isAlive()).isFalse();
			assertThat(result.get().getStatus()).isEqualTo(LosslessOfflineCallbackResult.Status.TIMEOUT);
		assertThat(result.get().isFailAction()).isFalse();
			assertThat(executions.get()).isZero();
		}
		finally {
			executor.releaseFirstWorker();
			executor.releaseSecondSubmission();
			runner.destroy();
		}
	}

	private LosslessProperties properties() {
		LosslessProperties properties = new LosslessProperties();
		properties.setOfflineCallbackTimeout(1000);
		properties.setOfflineCallbackTotalTimeout(2000);
		return properties;
	}

	private LosslessProperties timeoutProperties() {
		LosslessProperties properties = new LosslessProperties();
		properties.setOfflineCallbackTimeout(20);
		properties.setOfflineCallbackTotalTimeout(200);
		return properties;
	}

	private static Thread createThread(Runnable runnable) {
		Thread thread = new Thread(runnable, "test-lossless-callback");
		thread.setDaemon(true);
		return thread;
	}

	private LosslessOfflineContext context() {
		Instant startedAt = Instant.now();
		return new LosslessOfflineContext("test", "test-service", "127.0.0.1", 48080,
				Map.of(), startedAt, startedAt.plusMillis(2000));
	}

	@SuppressWarnings("unchecked")
	private static void gateThreadPublication(Runnable task) throws Exception {
		Field field = task.getClass().getDeclaredField("runningThread");
		field.setAccessible(true);
		AtomicReference<Thread> gatedReference = Mockito.mock(AtomicReference.class);
		Mockito.doAnswer(invocation -> {
			Thread thread = invocation.getArgument(0);
			if (thread != null) {
				GATED_THREAD_PUBLICATION.countDown();
				await(RELEASE_THREAD_PUBLICATION);
			}
			return invocation.callRealMethod();
		}).when(gatedReference).set(Mockito.any());
		field.set(task, gatedReference);
	}

	private static void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("test latch was interrupted", e);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class RunnerConfiguration {

		@Bean
		LosslessOfflineCallbackRunner losslessOfflineCallbackRunner(
				ObjectProvider<LosslessOfflineCallback> callbackProvider, ListableBeanFactory beanFactory) {
			LosslessProperties properties = new LosslessProperties();
			properties.setOfflineCallbackTimeout(1000);
			properties.setOfflineCallbackTotalTimeout(2000);
			return new LosslessOfflineCallbackRunner(callbackProvider, beanFactory, properties);
		}
	}

	private static final class BlockingAfterExecuteExecutor extends ThreadPoolExecutor {

		private final CountDownLatch workerBlocked = new CountDownLatch(1);

		private final CountDownLatch releaseWorker = new CountDownLatch(1);

		private final AtomicInteger completedTasks = new AtomicInteger();

		private BlockingAfterExecuteExecutor() {
			super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
					LosslessOfflineCallbackRunnerTest::createThread);
		}

		private boolean awaitWorkerBlocked() throws InterruptedException {
			return workerBlocked.await(2, TimeUnit.SECONDS);
		}

		private void releaseWorker() {
			releaseWorker.countDown();
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			if (completedTasks.incrementAndGet() == 2) {
				workerBlocked.countDown();
				boolean released = false;
				while (!released) {
					try {
						released = releaseWorker.await(2, TimeUnit.SECONDS);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						released = false;
					}
				}
			}
		}
	}

	private static final class CapturingExecutor extends ThreadPoolExecutor {

		private final AtomicReference<Runnable> task = new AtomicReference<>();

		private CountDownLatch taskCaptured = new CountDownLatch(1);

		private boolean captureNextTask = false;

		private CapturingExecutor() {
			super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
					LosslessOfflineCallbackRunnerTest::createThread);
		}

		private boolean awaitTaskCaptured() throws InterruptedException {
			return taskCaptured.await(2, TimeUnit.SECONDS);
		}

		private boolean awaitThreadPublication() throws InterruptedException {
			return GATED_THREAD_PUBLICATION.await(2, TimeUnit.SECONDS);
		}

		private void releaseThreadPublication() {
			RELEASE_THREAD_PUBLICATION.countDown();
		}

		private Runnable getTask() {
			return task.get();
		}

		private void startCapture() {
			captureNextTask = true;
		}

		@Override
		public void execute(Runnable command) {
			if (captureNextTask) {
				task.set(command);
				taskCaptured.countDown();
				return;
			}
			super.execute(command);
		}
	}

	private static final class CompletedBeforeWaitExecutor extends ThreadPoolExecutor {

		private CompletedBeforeWaitExecutor() {
			super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
					LosslessOfflineCallbackRunnerTest::createThread);
		}

		@Override
		public void execute(Runnable command) {
			Thread worker = createThread(command);
			worker.start();
			try {
				worker.join(2000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			assertThat(worker.isAlive()).isFalse();
		}
	}

	private static final class ExceptionalAfterDeadlineExecutor extends ThreadPoolExecutor {

		private ExceptionalAfterDeadlineExecutor() {
			super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
					LosslessOfflineCallbackRunnerTest::createThread);
		}

		@Override
		public void execute(Runnable command) {
			try {
				Field field = command.getClass().getDeclaredField("future");
				field.setAccessible(true);
				field.set(command, new CompletedBeforeGetFuture<>());
			}
			catch (ReflectiveOperationException e) {
				throw new IllegalStateException("failed to replace callback future", e);
			}
			Thread worker = createThread(command);
			worker.start();
		}
	}

	private static final class CompletedBeforeGetFuture<T> extends CompletableFuture<T> {

		private final CountDownLatch futureCompleted = new CountDownLatch(1);

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			await(futureCompleted);
			return super.get(timeout, unit);
		}

		@Override
		public boolean completeExceptionally(Throwable throwable) {
			boolean completed = super.completeExceptionally(throwable);
			futureCompleted.countDown();
			return completed;
		}

		@Override
		public boolean complete(T value) {
			boolean completed = super.complete(value);
			futureCompleted.countDown();
			return completed;
		}
	}

	private static final class LateWorkerStartExecutor extends ThreadPoolExecutor {

		private final CountDownLatch firstWorkerBlocked = new CountDownLatch(1);

		private final CountDownLatch releaseFirstWorker = new CountDownLatch(1);

		private final CountDownLatch secondSubmissionBlocked = new CountDownLatch(1);

		private final CountDownLatch releaseSecondSubmission = new CountDownLatch(1);

		private final CountDownLatch secondWorkerCompleted = new CountDownLatch(1);

		private final AtomicInteger submissions = new AtomicInteger();

		private final AtomicInteger completedTasks = new AtomicInteger();

		private LateWorkerStartExecutor() {
			super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
					LosslessOfflineCallbackRunnerTest::createThread);
		}

		private boolean awaitFirstWorkerBlocked() throws InterruptedException {
			return firstWorkerBlocked.await(2, TimeUnit.SECONDS);
		}

		private boolean awaitSecondSubmissionBlocked() throws InterruptedException {
			return secondSubmissionBlocked.await(2, TimeUnit.SECONDS);
		}

		private void releaseFirstWorker() {
			releaseFirstWorker.countDown();
		}

		private void releaseSecondSubmission() {
			releaseSecondSubmission.countDown();
		}

		private boolean awaitSecondWorkerCompleted() throws InterruptedException {
			return secondWorkerCompleted.await(2, TimeUnit.SECONDS);
		}

		@Override
		public void execute(Runnable command) {
			super.execute(command);
			if (submissions.incrementAndGet() == 3) {
				secondSubmissionBlocked.countDown();
				await(releaseSecondSubmission);
			}
		}

		@Override
		protected void afterExecute(Runnable runnable, Throwable throwable) {
			if (completedTasks.incrementAndGet() == 2) {
				firstWorkerBlocked.countDown();
				await(releaseFirstWorker);
			}
			if (completedTasks.get() == 3) {
				secondWorkerCompleted.countDown();
			}
		}
	}
}
