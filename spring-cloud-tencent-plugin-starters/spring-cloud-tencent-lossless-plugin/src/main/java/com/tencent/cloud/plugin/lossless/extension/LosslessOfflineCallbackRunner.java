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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Executes offline callbacks on one bounded worker.
 *
 * @author evelynwei
 */
final class LosslessOfflineCallbackRunner implements DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(LosslessOfflineCallbackRunner.class);

	private static final long SHUTDOWN_WAIT_MILLIS = 5000;

	private final ObjectProvider<LosslessOfflineCallback> callbackProvider;

	private final ListableBeanFactory beanFactory;

	private final LosslessProperties properties;

	private final Supplier<ExecutorService> executorFactory;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private final AtomicReference<OfflineTask<?>> activeTask = new AtomicReference<>();

	private volatile List<CallbackEntry> callbacks;

	private volatile ExecutorService executor;

	LosslessOfflineCallbackRunner(ObjectProvider<LosslessOfflineCallback> callbackProvider,
			ListableBeanFactory beanFactory, LosslessProperties properties) {
		this(callbackProvider, beanFactory, properties, LosslessOfflineCallbackRunner::createExecutor);
	}

	LosslessOfflineCallbackRunner(ObjectProvider<LosslessOfflineCallback> callbackProvider,
			ListableBeanFactory beanFactory, LosslessProperties properties,
			Supplier<ExecutorService> executorFactory) {
		this.callbackProvider = callbackProvider;
		this.beanFactory = beanFactory;
		this.properties = properties;
		this.executorFactory = executorFactory;
	}

	boolean hasCallbackBeans() {
		return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
				LosslessOfflineCallback.class, true, false).length > 0;
	}

	LosslessOfflineCallbackResult execute(LosslessOfflineContext context) {
		if (closed.get()) {
			return LosslessOfflineCallbackResult.of(LosslessOfflineCallbackResult.Status.CLOSED, null, true,
					0, 0, 0, 0);
		}
		if (Thread.currentThread().isInterrupted()) {
			LOGGER.error("Lossless offline callbacks are skipped because the caller is interrupted.");
			return callerInterruptedResult(callbacks == null ? 0 : callbacks.size(), 0);
		}
		if (activeTask.get() != null) {
			if (Thread.currentThread().isInterrupted()) {
				LOGGER.error("Lossless offline callbacks are skipped because the caller is interrupted.");
				return callerInterruptedResult(callbacks == null ? 0 : callbacks.size(), 0);
			}
			LosslessOfflineCallbackResult result = activeTaskResult(callbacks == null ? 0 : callbacks.size());
			LOGGER.error("Lossless offline callbacks are skipped because a previous task is still active.");
			return result;
		}

		long deadlineNanos = deadlineNanos(System.nanoTime(), properties.getOfflineCallbackTotalTimeout());
		List<CallbackEntry> entries = callbacks;
		if (entries == null) {
			long taskStartNanos = System.nanoTime();
			TaskExecution<List<CallbackEntry>> execution = executeTask("callback-resolution",
					() -> resolveCallbacks(), properties.getOfflineCallbackTotalTimeout(), taskStartNanos,
					deadlineNanos);
			if (execution.status() != TaskStatus.SUCCESS) {
				return taskExecutionResult(execution, 0, 0, 0, 0);
			}
			entries = execution.value();
			callbacks = entries;
		}
		if (entries.isEmpty()) {
			if (Thread.currentThread().isInterrupted()) {
				return LosslessOfflineCallbackResult.of(LosslessOfflineCallbackResult.Status.CALLER_INTERRUPTED,
						null, true, 0, 0, 0, 0);
			}
			return LosslessOfflineCallbackResult.success(0);
		}

		int successCount = 0;
		int failureCount = 0;
		for (int index = 0; index < entries.size(); index++) {
			long remainingNanos = remainingNanos(deadlineNanos, System.nanoTime());
			long remainingMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
			if (remainingNanos <= 0) {
				LOGGER.error("Lossless offline callback chain exhausted its total timeout of {} ms.",
						properties.getOfflineCallbackTotalTimeout());
				return chainResult(LosslessOfflineCallbackResult.Status.BUDGET_EXHAUSTED, null,
						successCount, failureCount, entries.size() - index);
			}

			CallbackEntry entry = entries.get(index);
			long waitMillis = Math.min(properties.getOfflineCallbackTimeout(), remainingMillis);
			LOGGER.info("Lossless offline callback [{}] started.", entry.name());
			long callbackStartNanos = System.nanoTime();
			TaskExecution<Void> execution = executeTask(entry.name(), () -> {
				entry.callback().onOffline(context);
				return null;
			}, waitMillis, callbackStartNanos, deadlineNanos);
			if (execution.status() == TaskStatus.SUCCESS) {
				successCount++;
				LOGGER.info("Lossless offline callback [{}] finished in {} ms.", entry.name(),
						TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - callbackStartNanos));
				continue;
			}
			if (execution.status() == TaskStatus.FAILED) {
				failureCount++;
				LOGGER.error("Lossless offline callback [{}] failed.", entry.name(), execution.failure());
				if (properties.getOfflineCallbackFailureMode() == OfflineCallbackFailureMode.ABORT) {
					return chainResult(LosslessOfflineCallbackResult.Status.CALLBACK_FAILED, execution.failure(),
							successCount, failureCount, entries.size() - index - 1);
				}
				continue;
			}
			return taskExecutionResult(execution, successCount, failureCount, 0,
					entries.size() - index - 1);
		}
		if (Thread.currentThread().isInterrupted()) {
			return LosslessOfflineCallbackResult.of(LosslessOfflineCallbackResult.Status.CALLER_INTERRUPTED,
					null, true, successCount, failureCount, 0, 0);
		}
		if (failureCount > 0) {
			return chainResult(LosslessOfflineCallbackResult.Status.CALLBACK_FAILED, null,
					successCount, failureCount, 0);
		}
		if (hasExpired(System.nanoTime(), deadlineNanos)) {
			return chainResult(LosslessOfflineCallbackResult.Status.BUDGET_EXHAUSTED, null,
					successCount, failureCount, 0);
		}
		return LosslessOfflineCallbackResult.success(successCount);
	}

	private List<CallbackEntry> resolveCallbacks() {
		List<LosslessOfflineCallback> orderedCallbacks = callbackProvider.orderedStream().toList();
		List<CallbackEntry> entries = new ArrayList<>(orderedCallbacks.size());
		for (LosslessOfflineCallback callback : orderedCallbacks) {
			entries.add(new CallbackEntry(callback.getClass().getName(), callback));
		}
		return List.copyOf(entries);
	}

	private LosslessOfflineCallbackResult activeTaskResult(int skippedCount) {
		boolean abort = properties.getOfflineCallbackFailureMode() == OfflineCallbackFailureMode.ABORT;
		return LosslessOfflineCallbackResult.of(LosslessOfflineCallbackResult.Status.TASK_ACTIVE, null, abort,
				0, 0, skippedCount, 0);
	}

	private LosslessOfflineCallbackResult callerInterruptedResult(int skippedCount, int notExecutedCount) {
		return LosslessOfflineCallbackResult.of(LosslessOfflineCallbackResult.Status.CALLER_INTERRUPTED, null,
				true, 0, 0, skippedCount, notExecutedCount);
	}

	private LosslessOfflineCallbackResult taskExecutionResult(TaskExecution<?> execution, int successCount,
			int failureCount, int skippedCount, int notExecutedCount) {
		boolean abort = properties.getOfflineCallbackFailureMode() == OfflineCallbackFailureMode.ABORT;
		boolean callerInterrupted = execution.status() == TaskStatus.CALLER_INTERRUPTED;
		boolean closed = execution.status() == TaskStatus.CLOSED;
		Throwable failure = execution.failure();
		LosslessOfflineCallbackResult.Status status;
		switch (execution.status()) {
			case TIMEOUT:
				status = LosslessOfflineCallbackResult.Status.TIMEOUT;
				break;
			case BUDGET_EXHAUSTED:
				status = LosslessOfflineCallbackResult.Status.BUDGET_EXHAUSTED;
				break;
			case REJECTED:
				status = LosslessOfflineCallbackResult.Status.SUBMIT_REJECTED;
				break;
			case ACTIVE:
				status = LosslessOfflineCallbackResult.Status.TASK_ACTIVE;
				break;
			case CLOSED:
				status = LosslessOfflineCallbackResult.Status.CLOSED;
				break;
			case CALLER_INTERRUPTED:
				status = LosslessOfflineCallbackResult.Status.CALLER_INTERRUPTED;
				break;
			case FAILED:
			default:
				status = LosslessOfflineCallbackResult.Status.CALLBACK_FAILED;
				break;
		}
		return LosslessOfflineCallbackResult.of(status, failure, abort || callerInterrupted || closed,
				successCount, failureCount, skippedCount, notExecutedCount);
	}

	private LosslessOfflineCallbackResult chainResult(LosslessOfflineCallbackResult.Status status, Throwable failure,
			int successCount, int failureCount, int notExecutedCount) {
		boolean abort = properties.getOfflineCallbackFailureMode() == OfflineCallbackFailureMode.ABORT;
		return LosslessOfflineCallbackResult.of(status, failure, abort, successCount, failureCount, 0,
				notExecutedCount);
	}

	private static long deadlineNanos(long startNanos, long timeoutMillis) {
		long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		long deadline = startNanos + timeoutNanos;
		// System.nanoTime may be positive, so a wrapped deadline means the wait exceeds Long.MAX_VALUE nanos.
		return deadline >= startNanos ? deadline : Long.MAX_VALUE;
	}

	private static long remainingNanos(long deadlineNanos, long nowNanos) {
		if (hasExpired(nowNanos, deadlineNanos)) {
			return 0;
		}
		long remaining = deadlineNanos - nowNanos;
		return remaining >= 0 ? remaining : Long.MAX_VALUE;
	}

	private static boolean hasExpired(long nowNanos, long deadlineNanos) {
		return nowNanos >= deadlineNanos;
	}

	private <T> TaskExecution<T> executeTask(String name, Callable<T> action, long timeoutMillis,
			long taskStartNanos, long deadlineNanos) {
		if (closed.get()) {
			return new TaskExecution<>(TaskStatus.CLOSED, null, null);
		}
		if (Thread.currentThread().isInterrupted()) {
			return new TaskExecution<>(TaskStatus.CALLER_INTERRUPTED, null, null);
		}
		long taskDeadlineNanos = deadlineNanos(taskStartNanos, timeoutMillis);
		taskDeadlineNanos = Math.min(taskDeadlineNanos, deadlineNanos);
		OfflineTask<T> task = new OfflineTask<>(name, action, activeTask, taskDeadlineNanos,
				properties.getOfflineCallbackTimeout(), deadlineNanos);
		ExecutorService taskExecutor;
		synchronized (this) {
			if (closed.get()) {
				return new TaskExecution<>(TaskStatus.CLOSED, null, null);
			}
			long now = System.nanoTime();
			if (hasExpired(now, deadlineNanos)) {
				return new TaskExecution<>(TaskStatus.BUDGET_EXHAUSTED, null, null);
			}
			if (hasExpired(now, taskDeadlineNanos)) {
				return new TaskExecution<>(TaskStatus.TIMEOUT, null, new TimeoutException());
			}
			if (!activeTask.compareAndSet(null, task)) {
				return new TaskExecution<>(TaskStatus.ACTIVE, null, null);
			}
			taskExecutor = executor;
			try {
				taskExecutor = getExecutor();
				taskExecutor.execute(task);
			}
			catch (Throwable failure) {
				task.cancel(false, taskExecutor);
				if (failure instanceof Error error) {
					throw error;
				}
				LOGGER.error("Lossless offline task [{}] was rejected by the callback worker.", name, failure);
				return new TaskExecution<>(TaskStatus.REJECTED, null, failure);
			}
		}

		try {
			if (Thread.currentThread().isInterrupted()) {
				cancelTask(task, true, taskExecutor);
				return new TaskExecution<>(TaskStatus.CALLER_INTERRUPTED, null, null);
			}
			long now = System.nanoTime();
			long remainingNanos = remainingNanos(deadlineNanos, now);
			if (remainingNanos <= 0) {
				cancelTask(task, true, taskExecutor);
				return new TaskExecution<>(TaskStatus.BUDGET_EXHAUSTED, null, null);
			}
			if (hasExpired(now, taskDeadlineNanos)) {
				cancelTask(task, true, taskExecutor);
				LOGGER.error("Lossless offline task [{}] exceeded its timeout before waiting.", name);
				return new TaskExecution<>(TaskStatus.TIMEOUT, null, new TimeoutException());
			}
			long waitNanos = Math.min(taskDeadlineNanos - now, remainingNanos);
			T value = task.future().get(waitNanos, TimeUnit.NANOSECONDS);
			now = System.nanoTime();
			if (hasExpired(now, deadlineNanos)) {
				return new TaskExecution<>(TaskStatus.BUDGET_EXHAUSTED, value, null);
			}
			if (hasExpired(now, taskDeadlineNanos)) {
				LOGGER.error("Lossless offline task [{}] completed after its timeout.", name);
				return new TaskExecution<>(TaskStatus.TIMEOUT, value, new TimeoutException());
			}
			return new TaskExecution<>(TaskStatus.SUCCESS, value, null);
		}
		catch (TimeoutException e) {
			cancelTask(task, true, taskExecutor);
			LOGGER.error("Lossless offline task [{}] timed out after {} ms.", name, timeoutMillis);
			return new TaskExecution<>(TaskStatus.TIMEOUT, null, e);
		}
		catch (InterruptedException e) {
			cancelTask(task, true, taskExecutor);
			Thread.currentThread().interrupt();
			return new TaskExecution<>(TaskStatus.CALLER_INTERRUPTED, null, e);
		}
		catch (CancellationException e) {
			return new TaskExecution<>(TaskStatus.CLOSED, null, e);
		}
		catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Error error) {
				throw error;
			}
			long now = System.nanoTime();
			if (hasExpired(now, deadlineNanos)) {
				return new TaskExecution<>(TaskStatus.BUDGET_EXHAUSTED, null, cause);
			}
			if (hasExpired(now, taskDeadlineNanos)) {
				LOGGER.error("Lossless offline task [{}] completed exceptionally after its timeout.", name);
				return new TaskExecution<>(TaskStatus.TIMEOUT, null, cause);
			}
			return new TaskExecution<>(TaskStatus.FAILED, null, cause);
		}
	}

	private ExecutorService getExecutor() {
		synchronized (this) {
			if (closed.get()) {
				return null;
			}
			ExecutorService result = executor;
			if (result == null) {
				result = executorFactory.get();
				executor = result;
			}
			return result;
		}
	}

	private void cancelTask(OfflineTask<?> task, boolean interrupt, ExecutorService taskExecutor) {
		// Keep queue release and activeTask cleanup in one critical section with task submission.
		synchronized (this) {
			task.cancel(interrupt, taskExecutor);
		}
	}

	private static ExecutorService createExecutor() {
		return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
				runnable -> {
					Thread thread = new Thread(runnable, "lossless-offline-callback");
					thread.setDaemon(true);
					return thread;
				}, new ThreadPoolExecutor.AbortPolicy());
	}

	@Override
	public void destroy() throws InterruptedException {
		ExecutorService currentExecutor = null;
		synchronized (this) {
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			OfflineTask<?> task = activeTask.get();
			currentExecutor = executor;
			if (task != null) {
				task.cancel(true, currentExecutor);
			}
			if (currentExecutor != null) {
				currentExecutor.shutdownNow();
			}
		}
		if (currentExecutor != null && !currentExecutor.awaitTermination(SHUTDOWN_WAIT_MILLIS,
				TimeUnit.MILLISECONDS)) {
			LOGGER.warn("Lossless offline callback worker did not stop within {} ms.", SHUTDOWN_WAIT_MILLIS);
		}
	}

	private record CallbackEntry(String name, LosslessOfflineCallback callback) {
	}

	private enum TaskStatus {
		SUCCESS,
		FAILED,
		TIMEOUT,
		REJECTED,
		ACTIVE,
		CLOSED,
		CALLER_INTERRUPTED,
		BUDGET_EXHAUSTED
	}

	private record TaskExecution<T>(TaskStatus status, T value, Throwable failure) {
	}

	private static final class OfflineTask<T> implements Runnable {

		private enum State {
			QUEUED,
			RUNNING,
			CANCELLED,
			EXITED
		}

		private final String name;

		private final Callable<T> action;

		private final CompletableFuture<T> future = new CompletableFuture<>();

		private final AtomicReference<State> state = new AtomicReference<>(State.QUEUED);

		private final AtomicReference<Thread> runningThread = new AtomicReference<>();

		private final AtomicReference<OfflineTask<?>> owner;

		private final long deadlineNanos;

		private final long timeoutMillis;

		private final long totalDeadlineNanos;

		private OfflineTask(String name, Callable<T> action, AtomicReference<OfflineTask<?>> owner,
				long deadlineNanos, long timeoutMillis, long totalDeadlineNanos) {
			this.name = name;
			this.action = action;
			this.owner = owner;
			this.deadlineNanos = deadlineNanos;
			this.timeoutMillis = timeoutMillis;
			this.totalDeadlineNanos = totalDeadlineNanos;
		}

		private CompletableFuture<T> future() {
			return future;
		}

		private boolean cancel(boolean interrupt, ExecutorService taskExecutor) {
			if (state.compareAndSet(State.QUEUED, State.CANCELLED)) {
				if (taskExecutor instanceof ThreadPoolExecutor threadPoolExecutor) {
					threadPoolExecutor.remove(this);
				}
				future.cancel(false);
				clearActiveTask();
				return true;
			}
			if (state.get() == State.RUNNING && !future.isDone()) {
				future.cancel(false);
				if (interrupt) {
					Thread thread = runningThread.get();
					if (thread != null) {
						thread.interrupt();
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public void run() {
			if (!start()) {
				return;
			}
			T value = null;
			Throwable failure = null;
			try {
				value = action.call();
			}
			catch (Throwable throwable) {
				failure = throwable;
			}
			finally {
				runningThread.set(null);
				state.set(State.EXITED);
				clearActiveTask();
			}
			if (failure != null) {
				future.completeExceptionally(failure);
			}
			else {
				future.complete(value);
			}
			if (future.isCancelled()) {
				LOGGER.error("Lossless offline task [{}] exited after cancellation.", name);
			}
		}

		private void clearActiveTask() {
			owner.compareAndSet(this, null);
		}

		private boolean start() {
			runningThread.set(Thread.currentThread());
			if (!state.compareAndSet(State.QUEUED, State.RUNNING)) {
				runningThread.set(null);
				return false;
			}
			if (future.isCancelled()) {
				runningThread.set(null);
				state.set(State.CANCELLED);
				clearActiveTask();
				return false;
			}
			long now = System.nanoTime();
			if (hasExpired(now, totalDeadlineNanos)) {
				LOGGER.error("Lossless offline task [{}] was not started because the total deadline expired.", name);
				finishExpired(new TimeoutException());
				return false;
			}
			if (hasExpired(now, deadlineNanos)) {
				LOGGER.error("Lossless offline task [{}] was not started because its timeout of {} ms expired.", name,
						timeoutMillis);
				finishExpired(new TimeoutException());
				return false;
			}
			return true;
		}

		private void finishExpired(TimeoutException failure) {
			runningThread.set(null);
			state.set(State.EXITED);
			clearActiveTask();
			future.completeExceptionally(failure);
		}
	}

}
