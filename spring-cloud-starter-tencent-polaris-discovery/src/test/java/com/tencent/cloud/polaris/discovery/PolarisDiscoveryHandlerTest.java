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

package com.tencent.cloud.polaris.discovery;

import com.tencent.cloud.polaris.PolarisDiscoveryProperties;
import com.tencent.cloud.polaris.context.PolarisSDKContextManager;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.ServicesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PolarisDiscoveryHandler}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(MockitoExtension.class)
public class PolarisDiscoveryHandlerTest {

	private PolarisDiscoveryHandler polarisDiscoveryHandler;

	@BeforeEach
	void setUp() {
		PolarisDiscoveryProperties polarisDiscoveryProperties = mock(PolarisDiscoveryProperties.class);
		doReturn(NAMESPACE_TEST).when(polarisDiscoveryProperties).getNamespace();

		ConsumerAPI consumerAPI = mock(ConsumerAPI.class);
		ServicesResponse servicesResponse = mock(ServicesResponse.class);
		doReturn(servicesResponse).when(consumerAPI).getServices(any());

		PolarisSDKContextManager polarisSDKContextManager = mock(PolarisSDKContextManager.class);
		doReturn(consumerAPI).when(polarisSDKContextManager).getConsumerAPI();
		polarisDiscoveryHandler = new PolarisDiscoveryHandler(polarisDiscoveryProperties, polarisSDKContextManager);
	}

	@Test
	public void testGetServices() throws PolarisException {
		ServicesResponse servicesResponse = polarisDiscoveryHandler.getServices();
		assertThat(servicesResponse).isNotNull();
	}
}
