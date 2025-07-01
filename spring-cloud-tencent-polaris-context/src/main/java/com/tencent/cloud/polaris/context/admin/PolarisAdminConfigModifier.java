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

package com.tencent.cloud.polaris.context.admin;

import com.tencent.cloud.common.constant.OrderConstant.Modifier;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;

/**
 * Config modifier for admin.
 *
 * @author Haotian Zhang
 */
public class PolarisAdminConfigModifier implements PolarisConfigModifier {

	private final PolarisAdminProperties polarisAdminProperties;

	public PolarisAdminConfigModifier(PolarisAdminProperties polarisAdminProperties) {
		this.polarisAdminProperties = polarisAdminProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		configuration.getGlobal().getAdmin().setHost(this.polarisAdminProperties.getHost());
		configuration.getGlobal().getAdmin().setPort(this.polarisAdminProperties.getPort());
	}

	@Override
	public int getOrder() {
		return Modifier.ADMIN_ORDER;
	}
}
