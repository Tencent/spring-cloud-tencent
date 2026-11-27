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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot used by offline callbacks.
 *
 * @author evelynwei
 */
public final class LosslessOfflineContext {

	private final String namespace;

	private final String service;

	private final String host;

	private final int port;

	private final Map<String, String> metadata;

	private final Instant startedAt;

	private final Instant deadline;

	LosslessOfflineContext(String namespace, String service, String host, int port, Map<String, String> metadata,
			Instant startedAt, Instant deadline) {
		this.namespace = namespace;
		this.service = service;
		this.host = host;
		this.port = port;
		this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
		this.startedAt = startedAt;
		this.deadline = deadline;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getService() {
		return service;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getDeadline() {
		return deadline;
	}
}
