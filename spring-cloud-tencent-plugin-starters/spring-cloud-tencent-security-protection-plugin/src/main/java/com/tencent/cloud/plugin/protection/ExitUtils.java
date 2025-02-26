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

package com.tencent.cloud.plugin.protection;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * ExitUtils.
 *
 * @author Shedfree Wu
 */
public final class ExitUtils {

	private ExitUtils() {

	}

	public static void exit(ApplicationContext context) {
		exit(context, 3000);
	}

	public static void exit(ApplicationContext context, int delay) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext) context;
			configurableContext.close();
		}

		try {
			Thread.sleep(delay);
		}
		catch (InterruptedException e) {
			// do nothing
		}
		System.exit(0);
	}
}
