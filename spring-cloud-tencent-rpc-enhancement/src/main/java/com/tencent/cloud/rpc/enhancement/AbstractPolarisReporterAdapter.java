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

package com.tencent.cloud.rpc.enhancement;

import java.util.List;
import java.util.Objects;

import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Abstract Polaris Reporter Adapter .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022-07-11
 */
public abstract class AbstractPolarisReporterAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractPolarisReporterAdapter.class);

	protected final RpcEnhancementProperties properties;

	/**
	 * Constructor With {@link RpcEnhancementProperties} .
	 *
	 * @param properties instance of {@link RpcEnhancementProperties}.
	 */
	protected AbstractPolarisReporterAdapter(RpcEnhancementProperties properties) {
		this.properties = properties;
	}

	/**
	 * Callback after completion of request processing, Check if business meltdown reporting is required.
	 *
	 * @param httpStatus request http status code
	 * @return true , otherwise return false .
	 */
	protected boolean apply(@Nullable HttpStatus httpStatus) {
		if (Objects.isNull(httpStatus)) {
			return false;
		}
		else {
			// statuses > series
			List<HttpStatus> status = properties.getStatuses();

			if (status.isEmpty()) {
				List<HttpStatus.Series> series = properties.getSeries();
				// Check INTERNAL_SERVER_ERROR (500) status.
				if (properties.isIgnoreInternalServerError() && Objects.equals(httpStatus, INTERNAL_SERVER_ERROR)) {
					return false;
				}
				if (series.isEmpty()) {
					return false;
				}
				else {
					try {
						return series.contains(HttpStatus.Series.valueOf(httpStatus));
					}
					catch (Exception e) {
						LOG.warn("Decode http status failed.", e);
					}
				}
			}
			else {
				// Use the user-specified fuse status code.
				return status.contains(httpStatus);
			}
		}
		// DEFAULT RETURN FALSE.
		return false;
	}
}
