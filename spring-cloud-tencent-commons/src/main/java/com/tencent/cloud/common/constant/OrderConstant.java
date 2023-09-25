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

package com.tencent.cloud.common.constant;

import org.springframework.core.Ordered;

import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;

/**
 * Constant for order.
 *
 * @author Haotian Zhang
 */
public class OrderConstant {

	public static class Client {
		/**
		 * Order constant for Feign.
		 */
		public static class Feign {
			/**
			 * Order of encode transfer metadata interceptor.
			 */
			public static final int ENCODE_TRANSFER_METADATA_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE - 1;

			/**
			 * Order of encode router label interceptor.
			 */
			public static final int ROUTER_LABEL_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * Order constant for RestTemplate.
		 */
		public static class RestTemplate {
			/**
			 * Order of encode transfer metadata interceptor.
			 */
			public static final int ENCODE_TRANSFER_METADATA_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE - 1;

			/**
			 * Order of encode router label interceptor.
			 */
			public static final int ROUTER_LABEL_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE;
		}

		/**
		 * Order constant for Spring Cloud Gateway.
		 */
		public static class Scg {
			/**
			 * Order of encode transfer metadata filter.
			 */
			public static final int ENCODE_TRANSFER_METADATA_FILTER_ORDER = LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;

			/**
			 * Order of enhanced filter.
			 */
			public static final int ENHANCED_FILTER_ORDER = LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
		}
	}

	public static class Server {
		/**
		 * Order constant for Servlet.
		 */
		public static class Servlet {
			/**
			 * Order of decode transfer metadata filter.
			 */
			public static final int DECODE_TRANSFER_METADATA_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 9;

			/**
			 * Order of enhanced filter.
			 */
			public static final int ENHANCED_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

			/**
			 * Order of rate-limit filter.
			 */
			public static final int RATE_LIMIT_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
		}

		/**
		 * Order constant for Reactive.
		 */
		public static class Reactive {
			/**
			 * Order of decode transfer metadata filter.
			 */
			public static final int DECODE_TRANSFER_METADATA_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 9;

			/**
			 * Order of enhanced filter.
			 */
			public static final int ENHANCED_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

			/**
			 * Order of rate-limit filter.
			 */
			public static final int RATE_LIMIT_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
		}
	}

	/**
	 * Order of configuration modifier.
	 */
	public static final class Modifier {

		/**
		 * Address modifier order.
		 */
		public static Integer ADDRESS_ORDER = Integer.MIN_VALUE;

		/**
		 * Discovery config modifier order.
		 */
		public static Integer DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE - 10;

		/**
		 * Nacos discovery config modifier order.
		 */
		public static Integer NACOS_DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE - 10;

		/**
		 * Consul discovery config modifier order.
		 */
		public static Integer CONSUL_DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE - 10;

		/**
		 * Order of discovery configuration modifier.
		 */
		public static Integer DISCOVERY_ORDER = 0;

		/**
		 * Order of circuit breaker configuration modifier.
		 */
		public static Integer CIRCUIT_BREAKER_ORDER = 2;

		/**
		 * Order of rate-limit configuration modifier.
		 */
		public static Integer RATE_LIMIT_ORDER = 2;

		/**
		 * Order of config configuration modifier.
		 */
		public static Integer CONFIG_ORDER = 1;

		/**
		 * Order of router configuration modifier.
		 */
		public static Integer ROUTER_ORDER = 1;

		/**
		 * Order of stat reporter configuration modifier.
		 */
		public static Integer STAT_REPORTER_ORDER = 1;

		/**
		 * Order of service contract configuration modifier.
		 */
		public static Integer SERVICE_CONTRACT_ORDER = Integer.MAX_VALUE - 9;
	}
}
