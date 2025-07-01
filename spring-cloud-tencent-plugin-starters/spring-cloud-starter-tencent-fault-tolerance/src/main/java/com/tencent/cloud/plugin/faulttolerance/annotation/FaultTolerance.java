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

package com.tencent.cloud.plugin.faulttolerance.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tencent.cloud.plugin.faulttolerance.model.FaultToleranceStrategy;

/**
 * Annotation for Fault Tolerance.
 *
 * @author Haotian Zhang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FaultTolerance {

	/**
	 * Specifies a method to process fallback logic.
	 * A fallback method should be defined in the same class where is @FaultTolerance.
	 * Also, a fallback method should have same signature to a method.
	 * for example:
	 * <p>
	 * <pre>@FaultTolerance(fallbackMethod  = "getByIdFallback").
	 * public String getById(String id) {
	 *     // original method implementation
	 * }
	 * public String getByIdFallback(String id) {
	 *     // fallback method implementation
	 * }
	 * </pre>
	 * </p>
	 * @return method name
	 */
	String fallbackMethod() default "";

	/**
	 * Defines exceptions which should be ignored.
	 *
	 * @return exceptions to ignore
	 */
	Class<? extends Throwable>[] ignoreExceptions() default {};

	/**
	 * Defines exceptions which should be retried.
	 * Default is all exceptions.
	 *
	 * @return exceptions to wrap
	 */
	Class<? extends Throwable>[] raisedExceptions() default {};

	/**
	 * Defines the fault tolerance strategy, the default is fast fail strategy.
	 * @see FaultToleranceStrategy
	 *
	 * @return the fault tolerance strategy
	 */
	FaultToleranceStrategy strategy() default FaultToleranceStrategy.FAIL_FAST;

	/**
	 * Number of retries, only used under {@link FaultToleranceStrategy}.FAIL_OVER strategy. Default is 0.
	 *
	 * @return the maximum number of retry attempts
	 */
	int maxAttempts() default 0;

	/**
	 * The parallelism of forking is only used under the {@link FaultToleranceStrategy}.FORKING strategy.
	 *
	 * @return the parallelism level
	 */
	int parallelism() default 1;
}
