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

import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.core.Ordered;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RIBBON_ROUTING_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

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
			 * {@link ReactiveLoadBalancerClientFilter}.LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150.
			 */
			public static final int ENCODE_TRANSFER_METADATA_FILTER_ORDER = 10150 + 1;

			/**
			 * Order of enhanced filter.
			 * {@link ReactiveLoadBalancerClientFilter}.LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150.
			 */
			public static final int ENHANCED_FILTER_ORDER = 10150 + 1;
		}

		/**
		 * Order constant for Zuul.
		 */
		public static class Zuul {

			/**
			 * Order of encode transfer metadata filter.
			 */
			public static final int ENCODE_TRANSFER_METADATA_FILTER_ORDER = RIBBON_ROUTING_FILTER_ORDER - 1;

			/**
			 * Order of enhanced ROUTE filter.
			 */
			public static final int ENHANCED_ROUTE_FILTER_ORDER = RIBBON_ROUTING_FILTER_ORDER + 1;

			/**
			 * Order of enhanced POST filter.
			 */
			public static final int ENHANCED_POST_FILTER_ORDER = SEND_RESPONSE_FILTER_ORDER + 1;

			/**
			 * Order of enhanced ERROR filter.
			 */
			public static final int ENHANCED_ERROR_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE;

			/**
			 * Order of circuit breaker post filter.
			 */
			public static final int CIRCUIT_BREAKER_POST_FILTER_ORDER = SEND_RESPONSE_FILTER_ORDER - 1;

			/**
			 * Order of circuit breaker filter.
			 */
			public static final int CIRCUIT_BREAKER_FILTER_ORDER = PRE_DECORATION_FILTER_ORDER + 2;
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
		public static Integer DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE;

		/**
		 * Nacos discovery config modifier order.
		 */
		public static Integer NACOS_DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE;

		/**
		 * Consul discovery config modifier order.
		 */
		public static Integer CONSUL_DISCOVERY_CONFIG_ORDER = Integer.MAX_VALUE;

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
	}
}
