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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.spi.InstanceMetadataProvider;
import com.tencent.cloud.common.spi.impl.DefaultInstanceMetadataProvider;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * test for {@link StaticMetadataManager}.
 *
 * @author lepdou 2022-06-27
 */
@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
public class StaticMetadataManagerTest {

	/**
	 * EnvironmentVariablesRule.
	 */
	@SystemStub
	public EnvironmentVariables environmentVariables = new EnvironmentVariables();
	@Mock
	private MetadataLocalProperties metadataLocalProperties;

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testParseConfigMetadata() {
		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v22");
		content.put("zone", "zone1");
		content.put("region", "region1");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));
		when(metadataLocalProperties.getDisposable()).thenReturn(Collections.singletonList("k1"));
		when(metadataLocalProperties.getHeaders()).thenReturn(Arrays.asList("a", "d"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties, null);

		Map<String, String> metadata = metadataManager.getAllConfigMetadata();
		assertThat(metadata.size()).isEqualTo(4);
		assertThat(metadata.get("k1")).isEqualTo("v1");
		assertThat(metadata.get("k2")).isEqualTo("v22");

		Map<String, String> transitiveMetadata = metadataManager.getConfigTransitiveMetadata();
		assertThat(transitiveMetadata.size()).isEqualTo(1);
		assertThat(transitiveMetadata.get("k1")).isEqualTo("v1");

		Map<String, String> disposableMetadata = metadataManager.getConfigDisposableMetadata();
		assertThat(disposableMetadata.size()).isEqualTo(1);
		assertThat(disposableMetadata.get("k1")).isEqualTo("v1");

		assertThat(metadataManager.getZone()).isEqualTo("zone1");
		assertThat(metadataManager.getRegion()).isEqualTo("region1");

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		assertThat(locationInfo.get("zone")).isEqualTo("zone1");
		assertThat(locationInfo.get("region")).isEqualTo("region1");

		String transHeaderFromConfig = metadataManager.getTransHeaderFromConfig();
		assertThat(transHeaderFromConfig).isEqualTo("a,d");
	}

	@Test
	public void testCustomSPIMetadata() {
		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties,
				Arrays.asList(new MockedMetadataProvider(), new DefaultInstanceMetadataProvider(null)));

		Map<String, String> metadata = metadataManager.getAllCustomMetadata();
		assertThat(metadata.size()).isEqualTo(5);
		assertThat(metadata.get("k1")).isEqualTo("v1");
		assertThat(metadata.get("k2")).isEqualTo("v22");
		assertThat(metadata.get("k3")).isEqualTo("v33");
		assertThat(metadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE)).isEqualTo(NAMESPACE_TEST);
		assertThat(metadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAME)).isEqualTo(SERVICE_PROVIDER);

		Map<String, String> transitiveMetadata = metadataManager.getCustomSPITransitiveMetadata();
		assertThat(transitiveMetadata.size()).isEqualTo(1);
		assertThat(transitiveMetadata.get("k2")).isEqualTo("v22");

		Map<String, String> disposableMetadata = metadataManager.getCustomSPIDisposableMetadata();
		assertThat(disposableMetadata.size()).isEqualTo(3);
		assertThat(disposableMetadata.get("k3")).isEqualTo("v33");
		assertThat(disposableMetadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE)).isEqualTo(NAMESPACE_TEST);
		assertThat(disposableMetadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAME)).isEqualTo(SERVICE_PROVIDER);

		assertThat(metadataManager.getZone()).isEqualTo("zone2");
		assertThat(metadataManager.getRegion()).isEqualTo("region1");

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		assertThat(locationInfo.get("zone")).isEqualTo("zone2");
		assertThat(locationInfo.get("region")).isEqualTo("region1");
	}

	@Test
	public void testMergedMetadata() {
		// set environment variables
		environmentVariables.set("SCT_TRAFFIC_CONTENT_RAW_TRANSHEADERS", "a,b,c,e");

		Map<String, String> content = new HashMap<>();
		content.put("k1", "v1");
		content.put("k2", "v2");
		content.put("zone", "zone1");
		content.put("region", "region1");
		content.put("campus", "campus1");

		when(metadataLocalProperties.getContent()).thenReturn(content);
		when(metadataLocalProperties.getTransitive()).thenReturn(Collections.singletonList("k1"));
		when(metadataLocalProperties.getHeaders()).thenReturn(Arrays.asList("b", "d"));

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties,
				Arrays.asList(new MockedMetadataProvider(), new DefaultInstanceMetadataProvider(null)));

		Map<String, String> metadata = metadataManager.getMergedStaticMetadata();
		assertThat(metadata.size()).isEqualTo(8);
		assertThat(metadata.get("k1")).isEqualTo("v1");
		assertThat(metadata.get("k2")).isEqualTo("v22");
		assertThat(metadata.get("k3")).isEqualTo("v33");
		assertThat(metadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE)).isEqualTo(NAMESPACE_TEST);
		assertThat(metadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAME)).isEqualTo(SERVICE_PROVIDER);

		Map<String, String> transitiveMetadata = metadataManager.getMergedStaticTransitiveMetadata();
		assertThat(transitiveMetadata.size()).isEqualTo(2);
		assertThat(transitiveMetadata.get("k1")).isEqualTo("v1");
		assertThat(transitiveMetadata.get("k2")).isEqualTo("v22");

		Map<String, String> disposableMetadata = metadataManager.getMergedStaticDisposableMetadata();
		assertThat(disposableMetadata.size()).isEqualTo(3);
		assertThat(disposableMetadata.get("k3")).isEqualTo("v33");
		assertThat(disposableMetadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE)).isEqualTo(NAMESPACE_TEST);
		assertThat(disposableMetadata.get(DEFAULT_METADATA_SOURCE_SERVICE_NAME)).isEqualTo(SERVICE_PROVIDER);

		assertThat(metadataManager.getAllEnvMetadata()).isEmpty();
		assertThat(metadataManager.getEnvTransitiveMetadata()).isEmpty();

		assertThat(metadataManager.getZone()).isEqualTo("zone2");
		assertThat(metadataManager.getRegion()).isEqualTo("region1");
		assertThat(metadataManager.getCampus()).isEqualTo("campus2");

		Map<String, String> locationInfo = metadataManager.getLocationMetadata();
		assertThat(locationInfo.get("zone")).isEqualTo("zone2");
		assertThat(locationInfo.get("region")).isEqualTo("region1");
		assertThat(locationInfo.get("campus")).isEqualTo("campus2");

		String transHeader = metadataManager.getTransHeader();
		assertThat(transHeader).isEqualTo("a,b,c,d,e");
	}

	@Test
	public void testEnvMetadata() {
		// set env
		environmentVariables.set("SCT_METADATA_CONTENT_TRANSITIVE", "transitiveKey")
				.set("SCT_METADATA_CONTENT_DISPOSABLE", "disposableKey")
				.set("SCT_METADATA_CONTENT_transitiveKey", "transitiveValue")
				.set("SCT_METADATA_CONTENT_disposableKey", "disposableValue")
				.set("SCT_TRAFFIC_CONTENT_RAW_TRANSHEADERS", "header1,header2,header3");

		StaticMetadataManager metadataManager = new StaticMetadataManager(metadataLocalProperties, null);
		Map<String, String> allEnvMetadata = metadataManager.getAllEnvMetadata();
		assertThat(allEnvMetadata).containsKey("transitiveKey");
		assertThat(allEnvMetadata).containsKey("disposableKey");

		Map<String, String> envDisposableMetadata = metadataManager.getEnvDisposableMetadata();
		assertThat(envDisposableMetadata).containsKey("disposableKey");
		assertThat(envDisposableMetadata.get("disposableKey")).isEqualTo("disposableValue");

		Map<String, String> envTransitiveMetadata = metadataManager.getEnvTransitiveMetadata();
		assertThat(envTransitiveMetadata).containsKey("transitiveKey");
		assertThat(envTransitiveMetadata.get("transitiveKey")).isEqualTo("transitiveValue");

		String transHeaderFromEnv = metadataManager.getTransHeaderFromEnv();
		assertThat(transHeaderFromEnv).isEqualTo("header1,header2,header3");
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
		public Set<String> getDisposableMetadataKeys() {
			Set<String> transitiveKeys = new HashSet<>();
			transitiveKeys.add("k3");
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
			return "campus2";
		}
	}
}
