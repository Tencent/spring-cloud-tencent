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

package com.tencent.tsf.gateway.core.model;

import java.util.List;


public class RequestTransformerPluginInfo {

	/**
	 * 流量匹配条件。当全部流量时，为 null 或空集合.
	 */
	private List<TransformerTag> filters;

	/**
	 * 改写流量行为集合.
	 */
	private List<TransformerAction> actions;

	public List<TransformerTag> getFilters() {
		return filters;
	}

	public void setFilters(List<TransformerTag> filters) {
		this.filters = filters;
	}

	public List<TransformerAction> getActions() {
		return actions;
	}

	public void setActions(List<TransformerAction> actions) {
		this.actions = actions;
	}

	@Override
	public String toString() {
		return "RequestTransformerPluginInfo{" +
				"filters=" + filters +
				", actions=" + actions +
				'}';
	}
}
