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

package com.tencent.cloud.polaris.loadbalancer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.polaris.loadbalancer")
public class PolarisLoadBalancerProperties {

	/**
	 * Shortest response time load balancer.
	 */
	public ShortestResponseTime shortestResponseTime = new ShortestResponseTime();

	ShortestResponseTime getShortestResponseTime() {
		return shortestResponseTime;
	}

	void setShortestResponseTime(ShortestResponseTime shortestResponseTime) {
		this.shortestResponseTime = shortestResponseTime;
	}

	@Override
	public String toString() {
		return "PolarisLoadBalancerProperties{" +
				"shortestResponseTime=" + shortestResponseTime +
				'}';
	}

	public static class ShortestResponseTime {
		/**
		 * Slide period in milliseconds. Default is 30s.
		 */
		private long slidePeriod = 30000;

		long getSlidePeriod() {
			return slidePeriod;
		}

		void setSlidePeriod(long slidePeriod) {
			this.slidePeriod = slidePeriod;
		}

		@Override
		public String toString() {
			return "ShortestResponseTime{" +
					"slidePeriod=" + slidePeriod +
					'}';
		}
	}
}
