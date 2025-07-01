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

package com.tencent.tsf.serviceregistry.watch;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Empty annotation. Compatible with old versions TSF SDK.
 * <p>
 * Deprecated since 2.0.0.0.
 *
 * @author Haotian Zhang
 */
@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ConsulServiceChangeListener {

	/**
	 * 监听的服务名.
	 * @return 监听的服务名
	 */
	String serviceName();

	/**
	 * 监听的命名空间，true 时为全局命名空间，false 为本命名空间.
	 * @return 监听的命名空间
	 */
	boolean global() default false;
}
