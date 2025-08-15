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

package com.tencent.cloud.polaris.context.config.extend.tsf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for polaris tls.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.tls")
public class TsfTlsProperties {

	/**
	 * default validity duration.
	 */
	public static final long DEFAULT_VALIDITY_DURATION = 30 * 24 * 60 * 60 * 1000L;
	/**
	 * default refresh before.
	 */
	public static final long DEFAULT_REFRESH_BEFORE = 24 * 60 * 60 * 1000L;
	/**
	 * default watch interval.
	 */
	public static final long DEFAULT_WATCH_INTERVAL = 60 * 60 * 1000L;

	private Long validityDuration = DEFAULT_VALIDITY_DURATION;

	private Long refreshBefore = DEFAULT_REFRESH_BEFORE;

	private Long watchInterval = DEFAULT_WATCH_INTERVAL;

	private Tsf tsf = new Tsf();

	public Long getValidityDuration() {
		return validityDuration;
	}

	public void setValidityDuration(Long validityDuration) {
		this.validityDuration = validityDuration;
	}

	public Long getRefreshBefore() {
		return refreshBefore;
	}

	public void setRefreshBefore(Long refreshBefore) {
		this.refreshBefore = refreshBefore;
	}

	public Long getWatchInterval() {
		return watchInterval;
	}

	public void setWatchInterval(Long watchInterval) {
		this.watchInterval = watchInterval;
	}

	public Tsf getTsf() {
		return tsf;
	}

	public void setTsf(Tsf tsf) {
		this.tsf = tsf;
	}

	@Override
	public String toString() {
		return "TsfTlsProperties{" +
				"validityDuration=" + validityDuration +
				", refreshBefore=" + refreshBefore +
				", watchInterval=" + watchInterval +
				", tsf=" + tsf +
				'}';
	}

	public static class Tsf {

		private String address;

		public String getAddress() {
			return address;
		}

		void setAddress(String address) {
			this.address = address;
		}

		@Override
		public String toString() {
			return "Tsf{" +
					"address='" + address + '\'' +
					'}';
		}
	}
}
