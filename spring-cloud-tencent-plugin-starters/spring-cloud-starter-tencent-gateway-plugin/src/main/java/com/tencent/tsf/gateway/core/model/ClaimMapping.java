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

import java.io.Serializable;

/**
 * @ClassName ClaimMapping
 * @Description TODO
 * @Author vmershen
 * @Date 2019/7/12 16:12
 * @Version 1.0
 */
public class ClaimMapping implements Serializable {


	private static final long serialVersionUID = -8768365572845806890L;
	//参数名称
	private String parameterName;
	//映射后参数名称
	private String mappingParameterName;
	//映射后参数名称
	private String location;

	public String getParameterName() {
		return parameterName;
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	public String getMappingParameterName() {
		return mappingParameterName;
	}

	public void setMappingParameterName(String mappingParameterName) {
		this.mappingParameterName = mappingParameterName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
