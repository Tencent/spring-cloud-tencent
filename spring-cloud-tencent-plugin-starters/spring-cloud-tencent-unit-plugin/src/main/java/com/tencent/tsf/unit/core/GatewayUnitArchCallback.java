/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
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
		if (!(o instanceof GatewayUnitArchCallback that)) {
			return false;
		}
		return StringUtils.equals(applicationName, that.applicationName) && StringUtils.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), applicationName);
	}

}
