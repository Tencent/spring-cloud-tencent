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

package org.springframework.aop.framework;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdkDynamicAopProxyUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdkDynamicAopProxyUtils.class);

	private JdkDynamicAopProxyUtils() {
	}

	public static Object getTarget(Object invocationHandler) {
		if (invocationHandler instanceof JdkDynamicAopProxy) {
			try {
				JdkDynamicAopProxy jdkDynamicAopProxy = (JdkDynamicAopProxy) invocationHandler;
				Field advisedField = JdkDynamicAopProxy.class.getDeclaredField("advised");
				advisedField.setAccessible(true);
				AdvisedSupport advisedSupport = (AdvisedSupport) advisedField.get(jdkDynamicAopProxy);

				if (advisedSupport != null && advisedSupport.getTargetSource() != null) {
					return advisedSupport.getTargetSource().getTarget();
				}
			}
			catch (Exception e) {
				LOGGER.error("Unexpected error occurred while getting target from JdkDynamicAopProxy", e);
			}
		}
		return null;
	}
}
