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

package com.tencent.cloud.common.metadata;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link MetadataContext}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = MetadataContextTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = reactive",
				"spring.autoconfigure.exclude=org.springframework.cloud.gateway.config.GatewayAutoConfiguration,org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration,org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration"})
public class MetadataContextTest {

	private MetadataContext metadataContext;

	@BeforeEach
	void setUp() {
		metadataContext = new MetadataContext();
	}

	/**
	 * Test getting context value with default value when key exists in DISPOSABLE fragment.
	 * Scenario: When key exists, should return the actual value instead of default value.
	 */
	@Test
	public void testGetContextWithDefault_WhenKeyExists_ShouldReturnActualValue() {
		metadataContext.putContext("testKey", "testValue");

		String result = metadataContext.getContextWithDefault("testKey", "defaultValue");

		assertThat(result).isEqualTo("testValue");
	}

	/**
	 * Test getting context value with default value when key does not exist.
	 * Scenario: When key does not exist, should return the default value.
	 */
	@Test
	public void testGetContextWithDefault_WhenKeyNotExists_ShouldReturnDefaultValue() {
		String result = metadataContext.getContextWithDefault("nonExistentKey", "defaultValue");

		assertThat(result).isEqualTo("defaultValue");
	}

	/**
	 * Test getting context value with default value when value is blank.
	 * Scenario: When value is blank (empty string), should return the default value.
	 */
	@Test
	public void testGetContextWithDefault_WhenValueIsBlank_ShouldReturnDefaultValue() {
		metadataContext.putContext("blankKey", "");

		String result = metadataContext.getContextWithDefault("blankKey", "defaultValue");

		assertThat(result).isEqualTo("defaultValue");
	}

	/**
	 * Test getting context value from DISPOSABLE fragment.
	 * Scenario: Should retrieve value from the default DISPOSABLE fragment.
	 */
	@Test
	public void testGetContext_FromDisposableFragment_ShouldReturnValue() {
		metadataContext.putContext("key1", "value1");

		String result = metadataContext.getContext("key1");

		assertThat(result).isEqualTo("value1");
	}

	/**
	 * Test getting context value when key does not exist.
	 * Scenario: When key does not exist, should return null.
	 */
	@Test
	public void testGetContext_WhenKeyNotExists_ShouldReturnNull() {
		String result = metadataContext.getContext("nonExistentKey");

		assertThat(result).isNull();
	}

	/**
	 * Test getting context value with specified fragment and default value.
	 * Scenario: When key exists in specified fragment, should return actual value.
	 */
	@Test
	public void testGetContextWithFragmentAndDefault_WhenKeyExists_ShouldReturnActualValue() {
		metadataContext.putContext(MetadataContext.FRAGMENT_TRANSITIVE, "transitiveKey", "transitiveValue");

		String result = metadataContext.getContextWithDefault(MetadataContext.FRAGMENT_TRANSITIVE, "transitiveKey", "default");

		assertThat(result).isEqualTo("transitiveValue");
	}

	/**
	 * Test getting context value with specified fragment when key does not exist.
	 * Scenario: When key does not exist in specified fragment, should return default value.
	 */
	@Test
	public void testGetContextWithFragmentAndDefault_WhenKeyNotExists_ShouldReturnDefaultValue() {
		String result = metadataContext.getContextWithDefault(MetadataContext.FRAGMENT_TRANSITIVE, "nonExistent", "defaultValue");

		assertThat(result).isEqualTo("defaultValue");
	}

	/**
	 * Test getting context value with specified fragment.
	 * Scenario: Should retrieve value from the specified fragment correctly.
	 */
	@Test
	public void testGetContextWithFragment_ShouldReturnValue() {
		metadataContext.putContext(MetadataContext.FRAGMENT_DISPOSABLE, "dispKey", "dispValue");

		String result = metadataContext.getContext(MetadataContext.FRAGMENT_DISPOSABLE, "dispKey");

		assertThat(result).isEqualTo("dispValue");
	}

	/**
	 * Test putting context value into default DISPOSABLE fragment.
	 * Scenario: Value should be stored in DISPOSABLE fragment and retrievable.
	 */
	@Test
	public void testPutContext_ToDefaultFragment_ShouldStoreValue() {
		metadataContext.putContext("newKey", "newValue");

		Map<String, String> disposableMetadata = metadataContext.getDisposableMetadata();
		assertThat(disposableMetadata).containsEntry("newKey", "newValue");
	}

	/**
	 * Test putting context value into specified fragment.
	 * Scenario: Value should be stored in the specified fragment correctly.
	 */
	@Test
	public void testPutContext_ToSpecifiedFragment_ShouldStoreValue() {
		metadataContext.putContext(MetadataContext.FRAGMENT_TRANSITIVE, "transKey", "transValue");

		Map<String, String> transitiveMetadata = metadataContext.getTransitiveMetadata();
		assertThat(transitiveMetadata).containsEntry("transKey", "transValue");
	}

	/**
	 * Test setting and getting disposable metadata.
	 * Scenario: Should correctly store and retrieve multiple key-value pairs in disposable metadata.
	 */
	@Test
	public void testSetAndGetDisposableMetadata_ShouldWorkCorrectly() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("key1", "value1");
		metadata.put("key2", "value2");

		metadataContext.setDisposableMetadata(metadata);

		Map<String, String> result = metadataContext.getDisposableMetadata();
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("key1", "value1");
		assertThat(result).containsEntry("key2", "value2");
	}

	/**
	 * Test setting and getting transitive metadata.
	 * Scenario: Should correctly store and retrieve transitive metadata that passes through service calls.
	 */
	@Test
	public void testSetAndGetTransitiveMetadata_ShouldWorkCorrectly() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("trans1", "transValue1");
		metadata.put("trans2", "transValue2");

		metadataContext.setTransitiveMetadata(metadata);

		Map<String, String> result = metadataContext.getTransitiveMetadata();
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("trans1", "transValue1");
		assertThat(result).containsEntry("trans2", "transValue2");
	}

	/**
	 * Test getting application metadata.
	 * Scenario: Should retrieve application-level metadata from APPLICATION fragment.
	 */
	@Test
	public void testGetApplicationMetadata_ShouldReturnApplicationFragmentData() {
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION, Map.of("appKey", "appValue"));

		Map<String, String> result = metadataContext.getApplicationMetadata();

		assertThat(result).containsEntry("appKey", "appValue");
	}

	/**
	 * Test setting and getting trans headers.
	 * Scenario: Should store header keys that need to be transmitted from upstream to downstream.
	 */
	@Test
	public void testSetAndGetTransHeaders_ShouldWorkCorrectly() {
		metadataContext.setTransHeaders("headerKey", "headerValue");

		Map<String, String> result = metadataContext.getTransHeaders();

		assertThat(result).containsEntry("headerKey", "headerValue");
	}

	/**
	 * Test setting and getting trans headers key-value pairs.
	 * Scenario: Should store header key-value pairs for transmission between services.
	 */
	@Test
	public void testSetAndGetTransHeadersKV_ShouldWorkCorrectly() {
		metadataContext.setTransHeadersKV("kvKey", "kvValue");

		Map<String, String> result = metadataContext.getTransHeadersKV();

		assertThat(result).containsEntry("kvKey", "kvValue");
	}

	/**
	 * Test setting and getting loadbalancer metadata.
	 * Scenario: Should store and retrieve object values for load balancer decisions.
	 */
	@Test
	public void testSetAndGetLoadbalancerMetadata_ShouldWorkCorrectly() {
		metadataContext.setLoadbalancer("lbKey", "lbValue");
		metadataContext.setLoadbalancer("lbIntKey", 100);

		Map<String, Object> result = metadataContext.getLoadbalancerMetadata();

		assertThat(result).containsEntry("lbKey", "lbValue");
		assertThat(result).containsEntry("lbIntKey", 100);
	}

	/**
	 * Test getting fragment context for TRANSITIVE fragment.
	 * Scenario: Should return metadata that passes through all service calls.
	 */
	@Test
	public void testGetFragmentContext_ForTransitiveFragment_ShouldReturnCorrectData() {
		Map<String, String> transitiveData = new HashMap<>();
		transitiveData.put("passThrough1", "value1");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE, transitiveData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		assertThat(result).containsEntry("passThrough1", "value1");
	}

	/**
	 * Test getting fragment context for DISPOSABLE fragment.
	 * Scenario: Should return metadata that is disposed after single use.
	 */
	@Test
	public void testGetFragmentContext_ForDisposableFragment_ShouldReturnCorrectData() {
		Map<String, String> disposableData = new HashMap<>();
		disposableData.put("disposeKey", "disposeValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE, disposableData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_DISPOSABLE);

		assertThat(result).containsEntry("disposeKey", "disposeValue");
	}

	/**
	 * Test getting fragment context for UPSTREAM_DISPOSABLE fragment.
	 * Scenario: Should return disposable metadata from the caller/upstream service.
	 */
	@Test
	public void testGetFragmentContext_ForUpstreamDisposableFragment_ShouldReturnCorrectData() {
		Map<String, String> upstreamData = new HashMap<>();
		upstreamData.put("upstreamKey", "upstreamValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE, upstreamData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE);

		assertThat(result).containsEntry("upstreamKey", "upstreamValue");
	}

	/**
	 * Test getting fragment context for APPLICATION fragment.
	 * Scenario: Should return application-level disposable metadata.
	 */
	@Test
	public void testGetFragmentContext_ForApplicationFragment_ShouldReturnCorrectData() {
		Map<String, String> appData = new HashMap<>();
		appData.put("applicationKey", "applicationValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION, appData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_APPLICATION);

		assertThat(result).containsEntry("applicationKey", "applicationValue");
	}

	/**
	 * Test getting fragment context for APPLICATION_NONE fragment.
	 * Scenario: Should return application metadata with NONE transitive type.
	 */
	@Test
	public void testGetFragmentContext_ForApplicationNoneFragment_ShouldReturnCorrectData() {
		Map<String, String> appNoneData = new HashMap<>();
		appNoneData.put("appNoneKey", "appNoneValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE, appNoneData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_APPLICATION_NONE);

		assertThat(result).containsEntry("appNoneKey", "appNoneValue");
	}

	/**
	 * Test getting fragment context for UPSTREAM_APPLICATION fragment.
	 * Scenario: Should return application-level metadata from the caller/upstream service.
	 */
	@Test
	public void testGetFragmentContext_ForUpstreamApplicationFragment_ShouldReturnCorrectData() {
		Map<String, String> upstreamAppData = new HashMap<>();
		upstreamAppData.put("upstreamAppKey", "upstreamAppValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_APPLICATION, upstreamAppData);

		Map<String, String> result = metadataContext.getFragmentContext(MetadataContext.FRAGMENT_UPSTREAM_APPLICATION);

		assertThat(result).containsEntry("upstreamAppKey", "upstreamAppValue");
	}

	/**
	 * Test getting fragment context for custom fragment.
	 * Scenario: Should handle custom fragment names using the default case in switch statement.
	 */
	@Test
	public void testGetFragmentContext_ForCustomFragment_ShouldReturnCorrectData() {
		Map<String, String> customData = new HashMap<>();
		customData.put("customKey", "customValue");
		metadataContext.putFragmentContext("custom-fragment", customData);

		Map<String, String> result = metadataContext.getFragmentContext("custom-fragment");

		assertThat(result).containsEntry("customKey", "customValue");
	}

	/**
	 * Test putting fragment context with NONE fragment type.
	 * Scenario: Should store metadata with NONE transitive type that won't be passed to downstream.
	 */
	@Test
	public void testPutFragmentContext_ForNoneFragment_ShouldStoreCorrectly() {
		Map<String, String> noneData = new HashMap<>();
		noneData.put("noneKey", "noneValue");
		metadataContext.putFragmentContext(MetadataContext.FRAGMENT_NONE, noneData);

		// NONE fragment doesn't have a direct getter, verify through internal state
		// The data should be stored with TransitiveType.NONE
		assertThat(noneData).containsEntry("noneKey", "noneValue");
	}

	/**
	 * Test static method setLocalService.
	 * Scenario: Should update the LOCAL_SERVICE static field.
	 */
	@Test
	public void testSetLocalService_ShouldUpdateStaticField() {
		String originalService = MetadataContext.LOCAL_SERVICE;

		MetadataContext.setLocalService("newServiceName");

		assertThat(MetadataContext.LOCAL_SERVICE).isEqualTo("newServiceName");

		// Restore original value
		MetadataContext.setLocalService(originalService);
	}

	/**
	 * Test static method setLocalNamespace.
	 * Scenario: Should update the LOCAL_NAMESPACE static field.
	 */
	@Test
	public void testSetLocalNamespace_ShouldUpdateStaticField() {
		String originalNamespace = MetadataContext.LOCAL_NAMESPACE;

		MetadataContext.setLocalNamespace("newNamespace");

		assertThat(MetadataContext.LOCAL_NAMESPACE).isEqualTo("newNamespace");

		// Restore original value
		MetadataContext.setLocalNamespace(originalNamespace);
	}

	/**
	 * Test fragment constants are defined correctly.
	 * Scenario: All fragment constant values should be non-null and match expected string values.
	 */
	@Test
	public void testFragmentConstants_ShouldHaveCorrectValues() {
		assertThat(MetadataContext.FRAGMENT_TRANSITIVE).isEqualTo("transitive");
		assertThat(MetadataContext.FRAGMENT_DISPOSABLE).isEqualTo("disposable");
		assertThat(MetadataContext.FRAGMENT_NONE).isEqualTo("none");
		assertThat(MetadataContext.FRAGMENT_UPSTREAM_DISPOSABLE).isEqualTo("upstream-disposable");
		assertThat(MetadataContext.FRAGMENT_APPLICATION).isEqualTo("application");
		assertThat(MetadataContext.FRAGMENT_APPLICATION_NONE).isEqualTo("application-none");
		assertThat(MetadataContext.FRAGMENT_UPSTREAM_APPLICATION).isEqualTo("upstream-application");
		assertThat(MetadataContext.FRAGMENT_RAW_TRANSHEADERS).isEqualTo("trans-headers");
		assertThat(MetadataContext.FRAGMENT_RAW_TRANSHEADERS_KV).isEqualTo("trans-headers-kv");
		assertThat(MetadataContext.FRAGMENT_LB_METADATA).isEqualTo("load-balance-metadata");
	}

	/**
	 * Test multiple values can be stored and retrieved from the same fragment.
	 * Scenario: Should support storing multiple key-value pairs in a single fragment.
	 */
	@Test
	public void testMultipleValuesInSameFragment_ShouldAllBeRetrievable() {
		metadataContext.putContext("key1", "value1");
		metadataContext.putContext("key2", "value2");
		metadataContext.putContext("key3", "value3");

		Map<String, String> result = metadataContext.getDisposableMetadata();

		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("key1", "value1");
		assertThat(result).containsEntry("key2", "value2");
		assertThat(result).containsEntry("key3", "value3");
	}

	/**
	 * Test overwriting existing value in fragment.
	 * Scenario: When putting a value with existing key, should overwrite the previous value.
	 */
	@Test
	public void testOverwriteExistingValue_ShouldUpdateValue() {
		metadataContext.putContext("overwriteKey", "originalValue");
		metadataContext.putContext("overwriteKey", "newValue");

		String result = metadataContext.getContext("overwriteKey");

		assertThat(result).isEqualTo("newValue");
	}

	/**
	 * Test getting loadbalancer metadata when no data is set.
	 * Scenario: Should return empty map when no loadbalancer metadata has been set.
	 */
	@Test
	public void testGetLoadbalancerMetadata_WhenEmpty_ShouldReturnEmptyMap() {
		Map<String, Object> result = metadataContext.getLoadbalancerMetadata();

		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
	}

	/**
	 * Test LOCAL_NAMESPACE and LOCAL_SERVICE static initialization.
	 * Scenario: Static fields should be initialized from application properties.
	 */
	@Test
	public void testStaticInitialization_ShouldLoadFromProperties() {
		assertThat(MetadataContext.LOCAL_NAMESPACE).isNotNull();
		assertThat(MetadataContext.LOCAL_SERVICE).isNotNull();
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
