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

package com.tencent.cloud.rpc.enhancement.plugin.assembly;

import java.util.Collection;

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedResponseContext;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ResponseContext;

/**
 * AssemblyResponseContext.
 *
 * @author sean yu
 */
public class AssemblyResponseContext implements ResponseContext {

	private EnhancedResponseContext responseContext;

	private Throwable throwable;

	private RetStatus retStatus;

	public AssemblyResponseContext() {
	}

	public AssemblyResponseContext(EnhancedResponseContext responseContext) {
		this.responseContext = responseContext;
	}

	@Override
	public Object getRetCode() {
		return this.responseContext.getHttpStatus();
	}

	@Override
	public String getHeader(String key) {
		return this.responseContext.getHttpHeaders().getFirst(key);
	}

	@Override
	public Collection<String> listHeaders() {
		return this.responseContext.getHttpHeaders().keySet();
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	@Override
	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setRetStatus(RetStatus retStatus) {
		this.retStatus = retStatus;
	}

	@Override
	public RetStatus getRetStatus() {
		return this.retStatus;
	}

}
