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

package com.tencent.tsf.gateway.core.model;


public class GatewayAllResult {

	private GroupResult groupResult;

	private GroupApiResult groupApiResult;

	private PathRewriteResult pathRewriteResult;

	private PathWildcardResult pathWildcardResult;

	public GatewayAllResult(GroupResult groupResult, GroupApiResult groupApiResult,
			PathRewriteResult pathRewriteResult, PathWildcardResult pathWildcardResult) {
		this.groupResult = groupResult;
		this.groupApiResult = groupApiResult;
		this.pathRewriteResult = pathRewriteResult;
		this.pathWildcardResult = pathWildcardResult;
	}

	public GroupResult getGroupResult() {
		return groupResult;
	}

	public void setGroupResult(GroupResult groupResult) {
		this.groupResult = groupResult;
	}

	public GroupApiResult getGroupApiResult() {
		return groupApiResult;
	}

	public void setGroupApiResult(GroupApiResult groupApiResult) {
		this.groupApiResult = groupApiResult;
	}

	public PathRewriteResult getPathRewriteResult() {
		return pathRewriteResult;
	}

	public void setPathRewriteResult(PathRewriteResult pathRewriteResult) {
		this.pathRewriteResult = pathRewriteResult;
	}

	public PathWildcardResult getPathWildcardResult() {
		return pathWildcardResult;
	}

	public void setPathWildcardResult(PathWildcardResult pathWildcardResult) {
		this.pathWildcardResult = pathWildcardResult;
	}
}
