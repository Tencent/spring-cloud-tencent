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
 *
 */

package com.tencent.cloud.polaris.context.listener;

import com.tencent.cloud.polaris.context.PolarisSDKContextManager;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

/**
 * @author frankjlli
 * @since 2023/9/18
 **/

public class PolarisContextApplicationEventListener implements ApplicationListener<ApplicationEvent> {

	private final PolarisSDKContextManager polarisSDKContextManager;

	public PolarisContextApplicationEventListener(PolarisSDKContextManager polarisSDKContextManager) {
		this.polarisSDKContextManager = polarisSDKContextManager;
	}

	@Override
	public void onApplicationEvent(@NonNull ApplicationEvent event) {
		if (event instanceof ApplicationFailedEvent) {
			//implicit invoke 'destroy' when the spring application fails to start, in order to stop non-daemon threads.
			polarisSDKContextManager.getSDKContext().destroy();
		}
	}
}
