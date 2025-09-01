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

package com.tencent.tsf.unit.core;

import java.util.Map;
import java.util.Objects;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.model.UnitArch.Gateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayUnitArchCallback implements IUnitChangeCallback {

	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayUnitArchCallback.class);
	private final String applicationName;

	public GatewayUnitArchCallback(String applicationName) {
		this.applicationName = applicationName;
	}

	@Override
	public void callback() {
		boolean enableUnit = false;
		for (Map.Entry<String, Gateway> entry : TencentUnitManager.getLocalBusinessSystemGwMap().entrySet()) {
			Gateway gateway = entry.getValue();
			if (StringUtils.equals(applicationName, gateway.getServiceName())
					&& StringUtils.equals(Env.getNamespaceId(), gateway.getNamespaceId())) {
				enableUnit = true;
				break;
			}
		}
		if (!enableUnit) {
			for (Map.Entry<String, Gateway> entry: TencentUnitManager.getLocalOnlyGwMap().entrySet()) {
				Gateway gateway = entry.getValue();
				if (StringUtils.equals(applicationName, gateway.getServiceName())
						&& StringUtils.equals(Env.getNamespaceId(), gateway.getNamespaceId())) {
					enableUnit = true;
					break;
				}
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[callback] enableUnit:{}, applicationName:{}, ns:{}, localBusinessSystemGwMap:{}, localOnlyGwMap:{}",
					enableUnit, applicationName, Env.getNamespaceId(), TencentUnitManager.getLocalBusinessSystemGwMap(), TencentUnitManager.getLocalOnlyGwMap());
		}
		if (TencentUnitManager.isEnable() != enableUnit) {
			LOGGER.info("[callback] unit enable change, new status:{}", enableUnit);
		}
		TencentUnitManager.setEnable(enableUnit);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof GatewayUnitArchCallback)) {
			return false;
		}
		GatewayUnitArchCallback that = (GatewayUnitArchCallback) o;
		return StringUtils.equals(applicationName, that.applicationName) && StringUtils.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), applicationName);
	}

}
