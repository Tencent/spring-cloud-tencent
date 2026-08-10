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

package com.tencent.cloud.rpc.enhancement.audit.config;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.polaris.context.PolarisConfigModifier;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.AuditLogConfigImpl;

/**
 * Config modifier for Polaris service call audit logging.
 *
 * @author Yuwei Fu
 */
public class AuditLogConfigModifier implements PolarisConfigModifier {

	private final PolarisAuditLogProperties auditLogProperties;

	public AuditLogConfigModifier(PolarisAuditLogProperties auditLogProperties) {
		this.auditLogProperties = auditLogProperties;
	}

	@Override
	public void modify(ConfigurationImpl configuration) {
		AuditLogConfigImpl auditLogConfig = configuration.getConsumer().getAuditLog();
		auditLogConfig.setEnable(auditLogProperties.isEnabled());
		auditLogConfig.setFormat(auditLogProperties.getFormat());
	}

	@Override
	public int getOrder() {
		return OrderConstant.Modifier.AUDIT_LOG_ORDER;
	}
}
