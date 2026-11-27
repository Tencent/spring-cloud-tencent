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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * Tracks the registry target used by every deregistration invocation of a strict action.
 */
public final class LosslessDeregistrationGuard {

	private static final Map<Registration, GuardContext> GUARDS = new ConcurrentHashMap<>();

	private LosslessDeregistrationGuard() {
	}

	static GuardContext begin(Registration registration, Object expectedTarget) {
		if (registration == null) {
			throw new IllegalArgumentException("registration must not be null");
		}
		GuardContext created = new GuardContext(registration, expectedTarget);
		GuardContext existing = GUARDS.putIfAbsent(registration, created);
		if (existing != null) {
			synchronized (existing) {
				if (existing.closed) {
					throw new IllegalStateException("Lossless deregistration guard is no longer registered.");
				}
				if (existing.expectedTarget != expectedTarget) {
					throw new IllegalStateException("Another lossless deregistration target is active.");
				}
				existing.depth++;
				return existing;
			}
		}
		return created;
	}

	public static void recordTarget(Registration registration, Object actualTarget) {
		if (registration == null) {
			return;
		}
		GuardContext context = GUARDS.get(registration);
		if (context == null) {
			return;
		}
		synchronized (context) {
			if (context.closed) {
				return;
			}
			if (actualTarget != context.expectedTarget) {
				context.changed = true;
				throw new IllegalStateException("Lossless deregistration target changed during the action.");
			}
		}
	}

	private static void end(GuardContext context) {
		synchronized (context) {
			context.depth--;
			if (context.depth > 0) {
				return;
			}
			context.closed = true;
			GUARDS.remove(context.registration, context);
			if (context.changed) {
				throw new IllegalStateException("Lossless deregistration target changed during the action.");
			}
		}
	}

	static final class GuardContext implements AutoCloseable {

		private final Object expectedTarget;

		private final Registration registration;

		private int depth = 1;

		private boolean changed;

		private boolean closed;

		private GuardContext(Registration registration, Object expectedTarget) {
			this.registration = registration;
			this.expectedTarget = expectedTarget;
		}

		@Override
		public void close() {
			end(this);
		}
	}

}
