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

package com.tencent.cloud.polaris.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuring the change listener annotation.
 * <p>This source file was reference from：
 * <code><a href=https://github.com/apolloconfig/apollo/blob/master/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/annotation/ApolloAnnotationProcessor.java>
 *     ApolloAnnotationProcessor</a></code>
 * @author Palmer Xu 2022-05-31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PolarisConfigKVFileChangeListener {

	/**
	 * The keys interested in the listener, will only be notified if any of the interested keys is changed.
	 * <p>
	 * If neither of {@code interestedKeys} and {@code interestedKeyPrefixes} is specified then the {@code listener} will be notified when any key is changed.
	 * @return interested keys in the listener
	 */
	String[] interestedKeys() default {};

	/**
	 * The key prefixes that the listener is interested in, will be notified if and only if the changed keys start with anyone of the prefixes.
	 * The prefixes will simply be used to determine whether the {@code listener} should be notified or not using {@code changedKey.startsWith(prefix)}.
	 * e.g. "spring." means that {@code listener} is interested in keys that starts with "spring.", such as "spring.banner", "spring.jpa", etc.
	 * and "application" means that {@code listener} is interested in keys that starts with "application", such as "applicationName", "application.port", etc.
	 * <p>
	 * If neither of {@code interestedKeys} and {@code interestedKeyPrefixes} is specified then the {@code listener} will be notified when whatever key is changed.
	 * @return interested key-prefixed in the listener
	 */
	String[] interestedKeyPrefixes() default {};
}
