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

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Snapshot of HttpServletRequest.
 *
 * @author Haotian Zhang
 */
public final class SnapshotHttpServletRequest implements HttpServletRequest {

	/**
	 * HTTP method.
	 */
	private final String method;

	/**
	 * Request URI.
	 */
	private final String requestURI;

	/**
	 * Query string.
	 */
	private final String queryString;

	/**
	 * HTTP headers.
	 */
	private final MultiValueMap<String, String> headers;

	/**
	 * HTTP cookies.
	 */
	private final Cookie[] cookies;

	private SnapshotHttpServletRequest(Builder builder) {
		this.method = builder.method;
		this.requestURI = builder.requestURI;
		this.queryString = builder.queryString;
		this.headers = new LinkedMultiValueMap<>(builder.headers);
		this.cookies = builder.cookies;
	}

	/**
	 * create SnapshotHttpServletRequest from HttpServletRequest.
	 *
	 * @param request original request
	 * @return snapshot
	 */
	public static SnapshotHttpServletRequest from(HttpServletRequest request) {
		Builder builder = new Builder()
				.method(request.getMethod())
				.requestURI(request.getRequestURI())
				.queryString(request.getQueryString())
				.cookies(request.getCookies());

		Enumeration<String> headerNames = request.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				Enumeration<String> headerValues = request.getHeaders(headerName);
				if (headerValues != null) {
					while (headerValues.hasMoreElements()) {
						builder.header(headerName, headerValues.nextElement());
					}
				}
			}
		}

		return builder.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getRequestURI() {
		return requestURI;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getHeader(String name) {
		// 返回第一个值
		return headers.getFirst(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> values = headers.get(name);
		if (values != null && !values.isEmpty()) {
			return Collections.enumeration(values);
		}
		return Collections.enumeration(Collections.emptyList());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(headers.keySet());
	}

	@Override
	public Cookie[] getCookies() {
		return cookies;
	}

	@Override
	public String getAuthType() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	// 以下是HttpServletRequest接口的其他方法，抛出UnsupportedOperationException

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getRemoteUser() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Principal getUserPrincipal() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public StringBuffer getRequestURL() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getServletPath() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public long getContentLengthLong() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getParameter(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Enumeration<String> getParameterNames() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String[] getParameterValues(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getProtocol() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getScheme() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getServerName() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public int getServerPort() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public BufferedReader getReader() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getRemoteHost() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public void setAttribute(String name, Object o) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public void removeAttribute(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isSecure() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public int getRemotePort() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getLocalAddr() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public int getLocalPort() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isAsyncStarted() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public boolean isAsyncSupported() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getRequestId() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getProtocolRequestId() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public ServletConnection getServletConnection() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Object getAttribute(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getCharacterEncoding() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public void setCharacterEncoding(String env) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public int getContentLength() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public String getContentType() {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public int getIntHeader(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	@Override
	public long getDateHeader(String name) {
		throw new UnsupportedOperationException("Snapshot request does not support this operation");
	}

	public static class Builder {
		private String method;
		private String requestURI;
		private String queryString;
		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		private Cookie[] cookies;

		public Builder method(String method) {
			this.method = method;
			return this;
		}

		public Builder requestURI(String requestURI) {
			this.requestURI = requestURI;
			return this;
		}

		public Builder queryString(String queryString) {
			this.queryString = queryString;
			return this;
		}

		public Builder header(String name, String value) {
			if (name != null && value != null) {
				this.headers.add(name, value);
			}
			return this;
		}

		public Builder cookies(Cookie[] cookies) {
			if (cookies != null) {
				this.cookies = cookies.clone();
			}
			return this;
		}

		public SnapshotHttpServletRequest build() {
			return new SnapshotHttpServletRequest(this);
		}
	}
}
