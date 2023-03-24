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
 */

package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PolarisCircuitBreaker annotation.
 * if coded fallback or fallbackClass provided, RestTemplate will always return fallback when any exception occurs,
 * if none coded fallback or fallbackClass provided, RestTemplate will return fallback response from Polaris server when fallback occurs.
 * fallback and fallbackClass cannot provide at same time.
 *
 * @author sean yu
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PolarisCircuitBreaker {

	/**
	 * a fallback string, will return a response { status: 200, body: fallback string} when any exception occurs.
	 *
	 * @return fallback string
	 */
	String fallback() default "";

	/**
	 * a fallback Class, will return a PolarisCircuitBreakerHttpResponse when any exception occurs.
	 * fallback Class must be a spring bean.
	 *
	 * @return PolarisCircuitBreakerFallback
	 */
	Class<? extends PolarisCircuitBreakerFallback> fallbackClass() default PolarisCircuitBreakerFallback.class;

}
