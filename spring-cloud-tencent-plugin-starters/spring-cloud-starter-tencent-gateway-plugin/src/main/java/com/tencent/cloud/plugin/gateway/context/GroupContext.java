/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.plugin.gateway.context;

import java.util.List;
import java.util.Map;

public class GroupContext {

	private String comment;

	private ContextPredicate predicate;

	private List<ContextRoute> routes;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public ContextPredicate getPredicate() {
		return predicate;
	}

	public void setPredicate(ContextPredicate predicate) {
		this.predicate = predicate;
	}

	public List<ContextRoute> getRoutes() {
		return routes;
	}

	public void setRoutes(List<ContextRoute> routes) {
		this.routes = routes;
	}

	public static class ContextPredicate {
		private ApiType apiType;

		private String context;

		private ContextNamespace namespace;

		private ContextService service;

		public ApiType getApiType() {
			return apiType;
		}

		public void setApiType(ApiType apiType) {
			this.apiType = apiType;
		}

		public ContextNamespace getNamespace() {
			return namespace;
		}

		public void setNamespace(ContextNamespace namespace) {
			this.namespace = namespace;
		}

		public ContextService getService() {
			return service;
		}

		public void setService(ContextService service) {
			this.service = service;
		}

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}

	}

	public static class ContextNamespace {
		private Position position;

		private String key;

		public Position getPosition() {
			return position;
		}

		public void setPosition(Position position) {
			this.position = position;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}

	public static class ContextService {
		private Position position;

		private String key;

		public Position getPosition() {
			return position;
		}

		public void setPosition(Position position) {
			this.position = position;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}

	public static class ContextRoute {
		private String path;

		private String pathMapping;

		private String method;

		private String service;

		private String host;

		private String namespace;

		private Map<String, String> metadata;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getPathMapping() {
			return pathMapping;
		}

		public void setPathMapping(String pathMapping) {
			this.pathMapping = pathMapping;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getService() {
			return service;
		}

		public void setService(String service) {
			this.service = service;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public Map<String, String> getMetadata() {
			return metadata;
		}

		public void setMetadata(Map<String, String> metadata) {
			this.metadata = metadata;
		}
	}
}
