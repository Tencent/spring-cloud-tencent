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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.tencent.cloud.plugin.lossless.config.LosslessProperties;
import com.tencent.polaris.api.pojo.BaseInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

/**
 * Creates lossless deregistration actions with optional business callbacks.
 *
 * @author evelynwei
 */
public class LosslessOfflineActionManager implements DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(LosslessOfflineActionManager.class);

	private final LosslessProperties properties;

	private final LosslessOfflineCallbackRunner callbackRunner;

	private final ReentrantLock actionLock = new ReentrantLock();

	private final AtomicLong actionIds = new AtomicLong();

	public LosslessOfflineActionManager(ObjectProvider<LosslessOfflineCallback> callbackProvider,
			ListableBeanFactory beanFactory, LosslessProperties properties) {
		this.properties = properties;
		this.callbackRunner = new LosslessOfflineCallbackRunner(callbackProvider, beanFactory, properties);
	}

	public Runnable createDeregisterAction(Registration registration, ServiceRegistry<Registration> serviceRegistry,
			BaseInstance instance) {
		return createDeregisterAction(registration, serviceRegistry, instance, null, true);
	}

	public Runnable createDeregisterAction(Registration registration, ServiceRegistry<Registration> serviceRegistry,
			BaseInstance instance, LosslessDeregistration strictDeregistration) {
		return createDeregisterAction(registration, serviceRegistry, instance, strictDeregistration, true);
	}

	public Runnable createDeregisterAction(Registration registration, ServiceRegistry<Registration> serviceRegistry,
			BaseInstance instance, LosslessDeregistration strictDeregistration,
			boolean strictDeregistrationRequired) {
		Runnable strictAction = new StrictDeregisterAction(registration, serviceRegistry, strictDeregistration,
				strictDeregistrationRequired);
		if (!properties.isOfflineCallbackEnabled() || !callbackRunner.hasCallbackBeans()) {
			return strictAction;
		}
		return new CallbackDeregisterAction(registration, serviceRegistry, instance, strictAction,
				actionIds.incrementAndGet());
	}

	@Override
	public void destroy() throws Exception {
		callbackRunner.destroy();
	}

	private final class StrictDeregisterAction implements Runnable {

		private final Registration registration;

		private final ServiceRegistry<Registration> serviceRegistry;

		private final LosslessDeregistration strictDeregistration;

		private final boolean strictDeregistrationRequired;

		private StrictDeregisterAction(Registration registration, ServiceRegistry<Registration> serviceRegistry,
				LosslessDeregistration strictDeregistration, boolean strictDeregistrationRequired) {
			this.registration = registration;
			this.serviceRegistry = serviceRegistry;
			this.strictDeregistration = strictDeregistration;
			this.strictDeregistrationRequired = strictDeregistrationRequired;
		}

		@Override
		public void run() {
			AopTargetUtils.TargetInfo targetInfo = AopTargetUtils.resolveTarget(serviceRegistry);
			Object expectedTarget = targetInfo.getTarget();
			LosslessDeregistration losslessDeregistration = resolveLosslessDeregistration(targetInfo);
			if (losslessDeregistration != null) {
				try (LosslessDeregistrationGuard.GuardContext guard =
						LosslessDeregistrationGuard.begin(registration, expectedTarget)) {
					LosslessDeregistrationResult result = losslessDeregistration.deregisterForLossless(registration, () -> {
						assertSameTarget(expectedTarget);
						serviceRegistry.deregister(registration);
						assertSameTarget(expectedTarget);
					});
					assertSameTarget(expectedTarget);
					assertLosslessDeregistrationResult(result);
				}
			}
			else {
				serviceRegistry.deregister(registration);
			}
		}

		private LosslessDeregistration resolveLosslessDeregistration(AopTargetUtils.TargetInfo targetInfo) {
			if (targetInfo.isDynamicTarget() && (strictDeregistration != null || strictDeregistrationRequired)) {
				throw new IllegalStateException(
						"Lossless deregistration is not available for a dynamic service registry target source.");
			}
			if (serviceRegistry instanceof LosslessDeregistration losslessDeregistration) {
				return losslessDeregistration;
			}
			Object target = targetInfo.getTarget();
			if (target instanceof LosslessDeregistration losslessDeregistration) {
				return losslessDeregistration;
			}
			if (targetInfo.isHotSwappable() && (strictDeregistration != null || strictDeregistrationRequired)) {
				throw new IllegalStateException(
						"Lossless deregistration is not available for the current hot-swappable target.");
			}
			if (strictDeregistration != null) {
				return strictDeregistration;
			}
			if (strictDeregistrationRequired) {
				throw new IllegalStateException(
						"Lossless deregistration is not available for the selected service registry.");
			}
			return null;
		}

		private void assertSameTarget(Object expectedTarget) {
			Object currentTarget = AopTargetUtils.getUltimateTarget(serviceRegistry);
			if (currentTarget != expectedTarget) {
				throw new IllegalStateException("Lossless deregistration target changed during the action.");
			}
		}

		private void assertLosslessDeregistrationResult(LosslessDeregistrationResult result) {
			if (result == null || !result.isExecuted()) {
				throw new IllegalStateException("Lossless deregistration was not executed.");
			}
			Exception failure = result.getFailure();
			if (failure instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (failure != null) {
				throw new IllegalStateException("Lossless deregistration failed.", failure);
			}
		}
	}

	private final class CallbackDeregisterAction implements Runnable {

		private final Registration registration;

		private final ServiceRegistry<Registration> serviceRegistry;

		private final BaseInstance instance;

		private final Runnable strictAction;

		private final long actionId;

		private CallbackDeregisterAction(Registration registration, ServiceRegistry<Registration> serviceRegistry,
				BaseInstance instance, Runnable strictAction, long actionId) {
			this.registration = registration;
			this.serviceRegistry = serviceRegistry;
			this.instance = instance;
			this.strictAction = strictAction;
			this.actionId = actionId;
		}

		@Override
		public void run() {
			if (!actionLock.tryLock()) {
				throw new IllegalStateException("Another lossless offline action is running.");
			}
			try {
				LosslessOfflineContext context = createContext();
				LosslessOfflineCallbackResult result = callbackRunner.execute(context);
				LOGGER.info("Lossless offline action [{}] callback result: status={}, successCount={}, "
								+ "failureCount={}, skippedCount={}, notExecutedCount={}.", actionId, result.getStatus(),
						result.getSuccessCount(),
						result.getFailureCount(), result.getSkippedCount(), result.getNotExecutedCount());
				if (result.getStatus() != LosslessOfflineCallbackResult.Status.SUCCESS) {
					LOGGER.error("Lossless offline action [{}] did not complete all callbacks.", actionId,
							result.getFailure());
				}
				if (result.isFailAction()) {
					throw new IllegalStateException("Lossless offline callbacks failed: " + result.getStatus(), result.getFailure());
				}
				if (Thread.currentThread().isInterrupted()) {
					LOGGER.error("Lossless offline action [{}] is interrupted before deregistration.", actionId);
					throw new IllegalStateException(
							"Lossless offline action was interrupted before deregistration.");
				}
				strictAction.run();
			}
			finally {
				actionLock.unlock();
			}
		}

		private LosslessOfflineContext createContext() {
			Instant startedAt = Instant.now();
			Instant deadline = startedAt.plusMillis(properties.getOfflineCallbackTotalTimeout());
			String namespace = instance.getNamespace();
			if (registration instanceof LosslessRegistrationNamespace registrationNamespace
					&& registrationNamespace.getLosslessNamespace() != null) {
				namespace = registrationNamespace.getLosslessNamespace();
			}
			return new LosslessOfflineContext(namespace, instance.getService(), instance.getHost(),
					registration.getPort(), registration.getMetadata(), startedAt, deadline);
		}
	}
}
