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

package com.tencent.cloud.plugin.gateway.context;

import java.util.List;
import java.util.Map;

import com.tencent.tsf.gateway.core.constant.AuthMode;

public class GroupContext {

	private String comment;

	private ContextPredicate predicate;

	private List<ContextRoute> routes;

	private ContextAuth auth;

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

	public ContextAuth getAuth() {
		return auth;
	}

	public void setAuth(ContextAuth auth) {
		this.auth = auth;
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

		public ContextNamespace() {

		}

		public ContextNamespace(Position position, String key) {
			this.position = position;
			this.key = key;
		}

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

		public ContextService() {

		}

		public ContextService(Position position, String key) {
			this.position = position;
			this.key = key;
		}

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

		private String apiId;

		private String service;

		private String host;

		private String namespace;

		private String namespaceId;

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

		public String getApiId() {
			return apiId;
		}

		public void setApiId(String apiId) {
			this.apiId = apiId;
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

		public String getNamespaceId() {
			return namespaceId;
		}

		public void setNamespaceId(String namespaceId) {
			this.namespaceId = namespaceId;
		}

		public Map<String, String> getMetadata() {
			return metadata;
		}

		public void setMetadata(Map<String, String> metadata) {
			this.metadata = metadata;
		}
	}

	public static class ContextAuth {
		private AuthMode type;

		private List<ContextSecret> secrets;

		public AuthMode getType() {
			return type;
		}

		public void setType(AuthMode type) {
			this.type = type;
		}

		public List<ContextSecret> getSecrets() {
			return secrets;
		}

		public void setSecrets(List<ContextSecret> secrets) {
			this.secrets = secrets;
		}
	}

	public static class ContextSecret {
		private String name;

		private String id;

		private String key;

		private String status;

		private String expiredTime;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getExpiredTime() {
			return expiredTime;
		}

		public void setExpiredTime(String expiredTime) {
			this.expiredTime = expiredTime;
		}
	}

}
