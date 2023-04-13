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

import org.springframework.http.HttpHeaders;

/**
 * EnhancedResponseContext.
 *
 * @author sean yu
 */
public class EnhancedResponseContext {

	private Integer httpStatus;

	private HttpHeaders httpHeaders;

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public void setHttpStatus(Integer httpStatus) {
		this.httpStatus = httpStatus;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public static EnhancedContextResponseBuilder builder() {
		return new EnhancedContextResponseBuilder();
	}

	@Override
	public String toString() {
		return "EnhancedResponseContext{" +
				"httpStatus=" + httpStatus +
				", httpHeaders=" + httpHeaders +
				'}';
	}

	public static final class EnhancedContextResponseBuilder {
		private Integer httpStatus;
		private HttpHeaders httpHeaders;

		private EnhancedContextResponseBuilder() {
		}

		public EnhancedContextResponseBuilder httpStatus(Integer httpStatus) {
			this.httpStatus = httpStatus;
			return this;
		}

		public EnhancedContextResponseBuilder httpHeaders(HttpHeaders httpHeaders) {
			this.httpHeaders = httpHeaders;
			return this;
		}

		public EnhancedResponseContext build() {
			EnhancedResponseContext enhancedResponseContext = new EnhancedResponseContext();
			enhancedResponseContext.setHttpStatus(httpStatus);
			enhancedResponseContext.setHttpHeaders(httpHeaders);
			return enhancedResponseContext;
		}
	}

}
