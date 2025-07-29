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

package com.tencent.cloud.polaris.context;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * Test for {@link PostInitPolarisSDKContext}.
 *
 * @author wh
 */
@ExtendWith(MockitoExtension.class)
public final class PostInitPolarisSDKContextTest {

	private static final String REGION = "region";
	private static final String ZONE = "zone";
	private static final String CAMPUS = "campus";
	@Mock
	private SDKContext sdkContext;
	@Mock
	private StaticMetadataManager staticMetadataManager;

	@Test
	public void testConstructor() {
		ValueContext valueContext = new ValueContext();

		when(sdkContext.getValueContext()).thenReturn(valueContext);
		when(staticMetadataManager.getRegion()).thenReturn(REGION);
		when(staticMetadataManager.getZone()).thenReturn(ZONE);
		when(staticMetadataManager.getCampus()).thenReturn(CAMPUS);

		new PostInitPolarisSDKContext(sdkContext, staticMetadataManager);
		String regionName = valueContext.getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION.name());
		String zoneName = valueContext.getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE.name());
		String campusName = valueContext.getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS.name());

		Assertions.assertThat(regionName).isEqualTo(REGION);
		Assertions.assertThat(zoneName).isEqualTo(ZONE);
		Assertions.assertThat(campusName).isEqualTo(CAMPUS);
	}
}

