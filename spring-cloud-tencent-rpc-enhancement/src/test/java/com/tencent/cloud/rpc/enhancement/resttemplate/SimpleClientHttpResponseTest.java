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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;


/**
 * Mock Test for {@link AbstractClientHttpResponse}.
 *
 * @author wh 2022/6/22
 */
public class SimpleClientHttpResponseTest extends AbstractClientHttpResponse {

	private final HttpURLConnection connection;

	@Nullable
	private HttpHeaders headers;

	@Nullable
	private InputStream responseStream;


	SimpleClientHttpResponseTest(HttpURLConnection connection) {
		this.connection = connection;
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.connection.getResponseCode();
	}

	@Override
	public String getStatusText() throws IOException {
		String result = this.connection.getResponseMessage();
		return (result != null) ? result : "";
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			// Header field 0 is the status line for most HttpURLConnections, but not on GAE
			String name = this.connection.getHeaderFieldKey(0);
			if (StringUtils.hasLength(name)) {
				this.headers.add(name, this.connection.getHeaderField(0));
			}
			int i = 1;
			while (true) {
				name = this.connection.getHeaderFieldKey(i);
				if (!StringUtils.hasLength(name)) {
					break;
				}
				this.headers.add(name, this.connection.getHeaderField(i));
				i++;
			}
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		InputStream errorStream = this.connection.getErrorStream();
		this.responseStream = (errorStream != null ? errorStream : this.connection.getInputStream());
		return this.responseStream;
	}

	@Override
	public void close() {
		try {
			if (this.responseStream == null) {
				getBody();
			}
			StreamUtils.drain(this.responseStream);
			this.responseStream.close();
		}
		catch (Exception ex) {
			// ignore
		}
	}
}
