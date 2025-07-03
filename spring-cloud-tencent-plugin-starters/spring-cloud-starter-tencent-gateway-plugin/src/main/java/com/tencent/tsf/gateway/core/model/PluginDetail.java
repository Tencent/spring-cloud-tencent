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


import java.util.List;
import java.util.Objects;

/**
 * @ClassName PluginDetail
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/2 11:53
 * @Version 1.0
 */
public class PluginDetail extends PluginInfo {

	//插件参数
	private List<PluginArgInfo> pluginArgInfos;

	public List<PluginArgInfo> getPluginArgInfos() {
		return pluginArgInfos;
	}

	public void setPluginArgInfos(List<PluginArgInfo> pluginArgInfos) {
		this.pluginArgInfos = pluginArgInfos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PluginDetail that)) {
			return false;
		}
		return Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPluginArgInfos());
	}
}
