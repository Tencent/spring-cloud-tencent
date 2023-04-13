/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * EnhancedRequestContext.
 *
 * @author sean yu
 */
public class EnhancedRequestContext {

	private HttpMethod httpMethod;

	private HttpHeaders httpHeaders;

	private URI url;

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public static EnhancedContextRequestBuilder builder() {
		return new EnhancedContextRequestBuilder();
	}

	@Override
	public String toString() {
		return "EnhancedRequestContext{" +
				"httpMethod=" + httpMethod +
				", httpHeaders=" + httpHeaders +
				", url=" + url +
				'}';
	}

	public static final class EnhancedContextRequestBuilder {
		private HttpMethod httpMethod;
		private HttpHeaders httpHeaders;
		private URI url;

		private EnhancedContextRequestBuilder() {
		}

		public EnhancedContextRequestBuilder httpMethod(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		public EnhancedContextRequestBuilder httpHeaders(HttpHeaders httpHeaders) {
			this.httpHeaders = httpHeaders;
			return this;
		}

		public EnhancedContextRequestBuilder url(URI url) {
			this.url = url;
			return this;
		}

		public EnhancedRequestContext build() {
			EnhancedRequestContext enhancedRequestContext = new EnhancedRequestContext();
			enhancedRequestContext.httpMethod = this.httpMethod;
			enhancedRequestContext.url = this.url;
			enhancedRequestContext.httpHeaders = this.httpHeaders;
			return enhancedRequestContext;
		}
	}

}
