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

package org.springframework.cloud.tsf.faulttolerance.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.tsf.faulttolerance.model.TsfFaultToleranceStragety;

/**
 * @author zhixinzxiliu
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Deprecated(since = "2.0.0.0")
public @interface TsfFaultTolerance {
	/**
	 * Specifies a method to process fallback logic.
	 * A fallback method should be defined in the same class where is TsfFaultTolerance.
	 * Also a fallback method should have same signature to a method.
	 * for example:
	 * <code>
	 *      &#064;TsfFaultTolerance(fallbackMethod  = "getByIdFallback")
	 *      public String getById(String id) {...}
	 *
	 *      public String getByIdFallback(String id) {...}
	 * </code>
	 *
	 * @return method name
	 */
	String fallbackMethod() default "";

	/**
	 * Specifies command properties.
	 *
	 * @return command properties
	 */
	TsfFaultToleranceProperty[] properties() default {};

	/**
	 * Defines exceptions which should be ignored.
	 *
	 * @return exceptions to ignore
	 */
	Class<? extends Throwable>[] ignoreExceptions() default {};

	/**
	 * Defines exceptions which should be retry.
	 * Default is all exceptions.
	 *
	 * @return exceptions to wrap
	 */
	Class<? extends Throwable>[] raisedExceptions() default {};

	/**
	 * 设置容错策略，默认为快速失败策略.
	 */
	TsfFaultToleranceStragety strategy() default TsfFaultToleranceStragety.FAIL_FAST;

	/**
	 * 重试次数，只在Failover策略下用到.
	 */
	int maxAttempts() default 0;

	/**
	 * forking 的并行度，只在Forking策略下用到.
	 */
	int parallelism() default 1;
}
