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

package com.tencent.cloud.metadata.pojo;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Snapshot of ServerHttpRequest.
 *
 * @author Haotian Zhang
 */
public final class SnapshotServerHttpRequest implements ServerHttpRequest {

	/**
	 * HTTP method.
	 */
	private final HttpMethod method;

	/**
	 * HTTP URI.
	 */
	private final URI uri;

	/**
	 * HTTP attributes.
	 */
	private final Map<String, Object> attributes;

	/**
	 * HTTP path.
	 */
	private final String path;

	/**
	 * HTTP headers.
	 */
	private final HttpHeaders headers;

	/**
	 * HTTP query params.
	 */
	private final MultiValueMap<String, String> queryParams;

	/**
	 * HTTP cookies.
	 */
	private final MultiValueMap<String, HttpCookie> cookies;

	/**
	 * HTTP remote address.
	 */
	private final InetSocketAddress remoteAddress;

	/**
	 * HTTP local address.
	 */
	private final InetSocketAddress localAddress;

	/**
	 * HTTP request id.
	 */
	private final String id;

	private SnapshotServerHttpRequest(Builder builder) {
		this.method = builder.method;
		this.uri = builder.uri;
		this.attributes = builder.attributes;
		this.path = builder.path;
		this.headers = HttpHeaders.readOnlyHttpHeaders(builder.headers);
		this.queryParams = new LinkedMultiValueMap<>(builder.queryParams);
		this.cookies = new LinkedMultiValueMap<>(builder.cookies);
		this.remoteAddress = builder.remoteAddress;
		this.localAddress = builder.localAddress;
		this.id = builder.id;
	}

	/**
	 * create SnapshotServerHttpRequest from ServerHttpRequest.
	 *
	 * @param request original request
	 * @return snapshot
	 */
	public static SnapshotServerHttpRequest from(ServerHttpRequest request) {
		return new Builder()
				.method(request.getMethod())
				.uri(request.getURI())
				.path(request.getPath().value())
				.headers(request.getHeaders())
				.queryParams(request.getQueryParams())
				.cookies(request.getCookies())
				.remoteAddress(request.getRemoteAddress())
				.localAddress(request.getLocalAddress())
				.id(request.getId())
				.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public RequestPath getPath() {
		return RequestPath.parse(path, null);
	}

	@Override
	public HttpHeaders getHeaders() {
		return headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return queryParams;
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		return cookies;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		throw new UnsupportedOperationException("Snapshot request does not support body access");
	}

	public static class Builder {
		private HttpMethod method;
		private URI uri;
		private Map<String, Object> attributes = new HashMap<>();
		private String path;
		private HttpHeaders headers = new HttpHeaders();
		private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		private MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		private InetSocketAddress remoteAddress;
		private InetSocketAddress localAddress;
		private String id;

		public Builder method(HttpMethod method) {
			this.method = method;
			return this;
		}

		public Builder uri(URI uri) {
			this.uri = uri;
			return this;
		}

		public Builder attributes(Map<String, Object> attributes) {
			this.attributes = attributes;
			return this;
		}

		public Builder path(String path) {
			this.path = path;
			return this;
		}

		public Builder headers(HttpHeaders headers) {
			if (headers != null) {
				this.headers = new HttpHeaders();
				this.headers.putAll(headers);
			}
			return this;
		}

		public Builder queryParams(MultiValueMap<String, String> queryParams) {
			if (queryParams != null) {
				this.queryParams = new LinkedMultiValueMap<>(queryParams);
			}
			return this;
		}

		public Builder cookies(MultiValueMap<String, HttpCookie> cookies) {
			if (cookies != null) {
				this.cookies = new LinkedMultiValueMap<>(cookies);
			}
			return this;
		}

		public Builder remoteAddress(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		public Builder localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public SnapshotServerHttpRequest build() {
			return new SnapshotServerHttpRequest(this);
		}
	}
}
