///*
// * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
// *
// * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
// *
// * Licensed under the BSD 3-Clause License (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://opensource.org/licenses/BSD-3-Clause
// *
// * Unless required by applicable law or agreed to in writing, software distributed
// * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// * CONDITIONS OF ANY KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations under the License.
// */
//
//package com.tencent.cloud.polaris.loadbalancer.reactive;
//
//import com.tencent.cloud.common.constant.HeaderConstant;
//import com.tencent.polaris.api.core.ConsumerAPI;
//
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
//import org.springframework.http.HttpHeaders;
//import org.springframework.web.reactive.function.client.ClientRequest;
//
//public class PolarisLoadBalancerClientRequestTransformer implements LoadBalancerClientRequestTransformer {
//
//	private final ConsumerAPI consumerAPI;
//
//	public PolarisLoadBalancerClientRequestTransformer(ConsumerAPI consumerAPI) {
//		this.consumerAPI = consumerAPI;
//	}
//
//	@Override
//	public ClientRequest transformRequest(ClientRequest request, ServiceInstance instance) {
//		if (instance != null) {
//			HttpHeaders headers = request.headers();
//			headers.add(HeaderConstant.INTERNAL_CALLEE_SERVICE_ID, instance.getServiceId());
//		}
//		return request;
//	}
//}
