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

package com.tencent.cloud.common.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.spi.InstanceMetadataProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.util.CollectionUtils;

import static org.mockito.Mockito.when;


/**
 * test for {@link StaticMetadataManager}.
 *@author lepdou 2022-06-27
 */
@RunWith(MockitoJUnitRunner.class)
public class StaticMetadataManagerTest {

	@Mock
	private MetadataLocalProperties metadataLocalProperties;

	@Test
	public void testParseConfigMetadata() {
		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v22");
		content.put("zone", "zone1");
		content.put("region", "region1");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties, null);

		Map<String, String> metadata = metadataManager.getAllConfigMetadata();
		Assert.assertEquals(4, metadata.size());
		Assert.assertEquals("v1", metadata.get("k1"));
		Assert.assertEquals("v22", metadata.get("k2"));

		Map<String, String> transitiveMetadata = metadataManager.getConfigTransitiveMetadata();
		Assert.assertEquals(1, transitiveMetadata.size());
		Assert.assertEquals("v1", transitiveMetadata.get("k1"));

		Assert.assertEquals("zone1", metadataManager.getZone());
		Assert.assertEquals("region1", metadataManager.getRegion());

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		Assert.assertEquals("zone1", locationInfo.get("zone"));
		Assert.assertEquals("region1", locationInfo.get("region"));
	}

	@Test
	public void testCustomSPIMetadata() {
		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties,
				new MockedMetadataProvider());

		Map<String, String> metadata = metadataManager.getAllCustomMetadata();
		Assert.assertEquals(3, metadata.size());
		Assert.assertEquals("v1", metadata.get("k1"));
		Assert.assertEquals("v22", metadata.get("k2"));
		Assert.assertEquals("v33", metadata.get("k3"));

		Map<String, String> transitiveMetadata = metadataManager.getCustomSPITransitiveMetadata();
		Assert.assertEquals(1, transitiveMetadata.size());
		Assert.assertEquals("v22", metadata.get("k2"));

		Assert.assertEquals("zone2", metadataManager.getZone());
		Assert.assertEquals("region1", metadataManager.getRegion());

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		Assert.assertEquals("zone2", locationInfo.get("zone"));
		Assert.assertEquals("region1", locationInfo.get("region"));
	}

	@Test
	public void testMergedMetadata() {
		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("zone", "zone1");
		content.put("region", "region1");
		content.put("campus", "campus1");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties,
				new MockedMetadataProvider());

		Map<String, String> metadata = metadataManager.getMergedStaticMetadata();
		Assert.assertEquals(6, metadata.size());
		Assert.assertEquals("v1", metadata.get("k1"));
		Assert.assertEquals("v22", metadata.get("k2"));
		Assert.assertEquals("v33", metadata.get("k3"));

		Map<String, String> transitiveMetadata = metadataManager.getMergedStaticTransitiveMetadata();
		Assert.assertEquals(2, transitiveMetadata.size());
		Assert.assertEquals("v1", metadata.get("k1"));
		Assert.assertEquals("v22", metadata.get("k2"));

		Assert.assertEquals("zone2", metadataManager.getZone());
		Assert.assertEquals("region1", metadataManager.getRegion());

		Assert.assertTrue(CollectionUtils.isEmpty(metadataManager.getAllEnvMetadata()));
		Assert.assertTrue(CollectionUtils.isEmpty(metadataManager.getEnvTransitiveMetadata()));

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		Assert.assertEquals("zone2", locationInfo.get("zone"));
		Assert.assertEquals("region1", locationInfo.get("region"));
		Assert.assertEquals("campus1", locationInfo.get("campus"));

	}

	static class MockedMetadataProvider implements InstanceMetadataProvider {

		@Override
		public Map<String, String> getMetadata() {
			Map<String, String> metadata = new HashMap<>();
			metadata.put("k1", "v1");
			metadata.put("k2", "v22");
			metadata.put("k3", "v33");
			return metadata;
		}

		@Override
		public Set<String> getTransitiveMetadataKeys() {
			Set<String> transitiveKeys = new HashSet<>();
			transitiveKeys.add("k2");
			return transitiveKeys;
		}

		@Override
		public String getRegion() {
			return "region1";
		}

		@Override
		public String getZone() {
			return "zone2";
		}

		@Override
		public String getCampus() {
			return null;
		}
	}
}
