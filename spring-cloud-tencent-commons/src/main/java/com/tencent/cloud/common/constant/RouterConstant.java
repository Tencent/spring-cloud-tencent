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

package com.tencent.cloud.common.constant;

/**
 * Router constants.
 *
 * @author lepdou, Hoatian Zhang
 */
public final class RouterConstant {

	/**
	 * the header of router label.
	 */
	public static final String ROUTER_LABEL_HEADER = "internal-router-label";
	/**
	 * the labels for rule router, contain transitive metadata.
	 */
	public static final String ROUTER_LABELS = "allMetadata";
	/**
	 * transitive labels.
	 */
	public static final String TRANSITIVE_LABELS = "transitiveMetadata";

	/**
	 * Default Private Constructor.
	 */
	private RouterConstant() {
	}
}
