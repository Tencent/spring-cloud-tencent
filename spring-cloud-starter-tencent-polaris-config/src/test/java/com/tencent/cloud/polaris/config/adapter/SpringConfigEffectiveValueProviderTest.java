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

package com.tencent.cloud.polaris.config.adapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.core.ConfigKeyConflict;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.bootstrap.config.BootstrapPropertySource;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SpringConfigEffectiveValueProvider}.
 *
 * @author evelynwei
 */
@ExtendWith(MockitoExtension.class)
class SpringConfigEffectiveValueProviderTest {

	private static final String NAMESPACE = "default";

	private static final String GROUP = "order-service";

	private static final String FILE_NAME = "application.yaml";

	private StandardEnvironment environment;

	private SpringConfigEffectiveValueProvider provider;

	@BeforeEach
	void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
		environment = new StandardEnvironment();
		provider = new SpringConfigEffectiveValueProvider(environment);
	}

	@AfterEach
	void tearDown() {
		PolarisPropertySourceManager.clearPropertySources();
	}

	/**
	 * Registers a file-dimension PolarisPropertySource into both the Environment chain
	 * (at the tail, below system properties like the real runtime) and the static manager.
	 */
	private PolarisPropertySource addFileSource(String namespace, String group, String fileName,
			Map<String, String> props) {
		ConfigKVFile kvFile = mockConfigKVFile(namespace, group, fileName, props);
		PolarisPropertySource source = new PolarisPropertySource(namespace, group, fileName, kvFile,
				new HashMap<String, Object>(props));
		PolarisPropertySourceManager.addPropertySource(source);
		environment.getPropertySources().addLast(source);
		return source;
	}

	private ConfigKVFile mockConfigKVFile(String namespace, String group, String fileName,
			Map<String, String> props) {
		ConfigKVFile kvFile = mock(ConfigKVFile.class);
		lenient().when(kvFile.getNamespace()).thenReturn(namespace);
		lenient().when(kvFile.getFileGroup()).thenReturn(group);
		lenient().when(kvFile.getFileName()).thenReturn(fileName);
		lenient().when(kvFile.getPropertyNames()).thenReturn(props.keySet());
		lenient().when(kvFile.getProperty(anyString(), any())).thenAnswer(invocation -> {
			String key = invocation.getArgument(0);
			return props.getOrDefault(key, invocation.getArgument(1));
		});
		return kvFile;
	}

	private ConfigFileMetadata metadata(String namespace, String group, String fileName) {
		return new DefaultConfigFileMetadata(namespace, group, fileName);
	}

	@Test
	void testCommandLineArgsOverridesPolarisValue() {
		addFileSource(NAMESPACE, GROUP, FILE_NAME, Collections.singletonMap("server.port", "8080"));
		environment.getPropertySources().addFirst(new MapPropertySource("commandLineArgs",
				Collections.singletonMap("server.port", "9090")));

		EffectiveValue value = provider.resolve("server.port", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		assertThat(value.getFileValue()).isEqualTo("8080");
		assertThat(value.getEffectiveValue()).isEqualTo("9090");
		assertThat(value.getPropertySource()).isEqualTo("commandLineArgs");
	}

	@Test
	void testEffectiveValueResolvesPlaceholder() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("server.port", "${http.port:8080}");
		props.put("http.port", "8081");
		addFileSource(NAMESPACE, GROUP, FILE_NAME, props);

		EffectiveValue value = provider.resolve("server.port", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		// file value keeps the raw placeholder, effective value is what the application reads
		assertThat(value.getFileValue()).isEqualTo("${http.port:8080}");
		assertThat(value.getEffectiveValue()).isEqualTo("8081");
		// still sourced from the polaris file itself: normalized coordinate
		assertThat(value.getPropertySource()).isEqualTo("polaris:default/order-service/application.yaml");
	}

	@Test
	void testGetKeysSortedAndComplete() {
		Map<String, String> props = new LinkedHashMap<>();
		props.put("b.key", "2");
		props.put("a.key", "1");
		props.put("c.key", "3");
		addFileSource(NAMESPACE, GROUP, FILE_NAME, props);

		List<String> keys = provider.getKeys(metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(keys).containsExactly("a.key", "b.key", "c.key");
		// unknown coordinate: empty list, and SDK will omit the whole properties field
		assertThat(provider.getKeys(metadata(NAMESPACE, GROUP, "unknown.yaml"))).isEmpty();
	}

	@Test
	void testConflictsAcrossMultipleWatchedFiles() {
		addFileSource(NAMESPACE, GROUP, FILE_NAME, Collections.singletonMap("k", "1"));
		addFileSource(NAMESPACE, "common", "common.yaml", Collections.singletonMap("k", "2"));

		List<ConfigKeyConflict> conflicts = provider.resolveConflicts("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(conflicts).hasSize(1);
		ConfigKeyConflict conflict = conflicts.get(0);
		assertThat(conflict.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(conflict.getGroup()).isEqualTo("common");
		assertThat(conflict.getFileName()).isEqualTo("common.yaml");
		assertThat(conflict.getValue()).isEqualTo("2");

		// querying from the other side excludes itself and reports application.yaml
		List<ConfigKeyConflict> reverse = provider.resolveConflicts("k", metadata(NAMESPACE, "common", "common.yaml"));
		assertThat(reverse).hasSize(1);
		assertThat(reverse.get(0).getFileName()).isEqualTo(FILE_NAME);
		assertThat(reverse.get(0).getValue()).isEqualTo("1");
	}

	@Test
	void testCompositeConfigFileExpanded() {
		// group-dimension loading: sub files merged into a CompositeConfigFile, first file wins
		ConfigKVFile subA = mockConfigKVFile(NAMESPACE, "mygroup", "a.yaml", Collections.singletonMap("k", "1"));
		ConfigKVFile subB = mockConfigKVFile(NAMESPACE, "mygroup", "b.yaml", Collections.singletonMap("k", "2"));
		CompositeConfigFile composite = new CompositeConfigFile(Arrays.asList(subA, subB));
		Map<String, Object> merged = new HashMap<>();
		merged.put("k", "1");
		PolarisPropertySource groupSource = new PolarisPropertySource(NAMESPACE, "mygroup", "", composite, merged);
		PolarisPropertySourceManager.addPropertySource(groupSource);
		environment.getPropertySources().addLast(groupSource);

		// query the losing sub file b.yaml
		EffectiveValue value = provider.resolve("k", metadata(NAMESPACE, "mygroup", "b.yaml"));

		assertThat(value).isNotNull();
		// file value from sub file b, effective value from the merged group map (a wins)
		assertThat(value.getFileValue()).isEqualTo("2");
		assertThat(value.getEffectiveValue()).isEqualTo("1");
		// property source points to the winning sub file, not the opaque group source name
		assertThat(value.getPropertySource()).isEqualTo("polaris:default/mygroup/a.yaml");

		// conflicts are sub-file grained: b is excluded, a is reported
		List<ConfigKeyConflict> conflicts = provider.resolveConflicts("k", metadata(NAMESPACE, "mygroup", "b.yaml"));
		assertThat(conflicts).hasSize(1);
		assertThat(conflicts.get(0).getFileName()).isEqualTo("a.yaml");
		assertThat(conflicts.get(0).getValue()).isEqualTo("1");

		// getKeys finds the sub file inside the composite
		assertThat(provider.getKeys(metadata(NAMESPACE, "mygroup", "a.yaml"))).containsExactly("k");
	}

	@Test
	void testResolveKeepsFileValueWhenEffectiveResolutionFails() {
		// environment.getProperty 抛异常(如不可解析占位符):生效值维度降级为 null,
		// 但已到手的文件原始值照常返回
		addFileSource(NAMESPACE, GROUP, FILE_NAME, Collections.singletonMap("k", "file-v"));
		ConfigurableEnvironment broken = mock(ConfigurableEnvironment.class);
		when(broken.getProperty(anyString()))
				.thenThrow(new IllegalArgumentException("Could not resolve placeholder"));
		SpringConfigEffectiveValueProvider failingProvider = new SpringConfigEffectiveValueProvider(broken);

		EffectiveValue value = failingProvider.resolve("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		assertThat(value.getFileValue()).isEqualTo("file-v");
		assertThat(value.getEffectiveValue()).isNull();
		assertThat(value.getPropertySource()).isNull();
	}

	@Test
	void testConflictsSkipsBrokenFile() {
		// 一个坏文件(getProperty 抛异常)不应丢弃已从健康文件收集的冲突
		addFileSource(NAMESPACE, "common", "common.yaml", Collections.singletonMap("k", "2"));
		ConfigKVFile broken = mock(ConfigKVFile.class);
		lenient().when(broken.getNamespace()).thenReturn(NAMESPACE);
		lenient().when(broken.getFileGroup()).thenReturn("broken-group");
		lenient().when(broken.getFileName()).thenReturn("broken.yaml");
		when(broken.getProperty(anyString(), any())).thenThrow(new RuntimeException("boom"));
		PolarisPropertySource brokenSource = new PolarisPropertySource(NAMESPACE, "broken-group", "broken.yaml",
				broken, new HashMap<String, Object>());
		PolarisPropertySourceManager.addPropertySource(brokenSource);

		List<ConfigKeyConflict> conflicts = provider.resolveConflicts("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(conflicts).hasSize(1);
		assertThat(conflicts.get(0).getFileName()).isEqualTo("common.yaml");
		assertThat(conflicts.get(0).getValue()).isEqualTo("2");
	}

	@Test
	void testConflictsDedupWhenWatchedAtBothDimensions() {
		// 同一文件同时以文件维度与 group 维度监听注册:冲突按坐标去重
		Map<String, String> props = Collections.singletonMap("k", "2");
		addFileSource(NAMESPACE, "common", "common.yaml", props);
		ConfigKVFile sub = mockConfigKVFile(NAMESPACE, "common", "common.yaml", props);
		CompositeConfigFile composite = new CompositeConfigFile(Collections.singletonList(sub));
		PolarisPropertySource groupSource = new PolarisPropertySource(NAMESPACE, "common", "", composite,
				new HashMap<String, Object>(props));
		PolarisPropertySourceManager.addPropertySource(groupSource);

		List<ConfigKeyConflict> conflicts = provider.resolveConflicts("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(conflicts).hasSize(1);
		assertThat(conflicts.get(0).getFileName()).isEqualTo("common.yaml");
		assertThat(conflicts.get(0).getValue()).isEqualTo("2");
	}

	@Test
	void testBootstrapWrappedSourcesUnwrapped() {
		// bootstrap 模式:PolarisPropertySource 被 CompositePropertySource("polaris-config") 聚合,
		// 外层再被 BootstrapPropertySource 包装——来源解析需递归解包出坐标
		PolarisPropertySource polarisSource = addFileSource(NAMESPACE, GROUP, FILE_NAME,
				Collections.singletonMap("k", "1"));
		environment.getPropertySources().remove(polarisSource.getName());
		CompositePropertySource composite = new CompositePropertySource("polaris-config");
		composite.addPropertySource(polarisSource);
		environment.getPropertySources().addLast(new BootstrapPropertySource<>(composite));

		EffectiveValue value = provider.resolve("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		assertThat(value.getEffectiveValue()).isEqualTo("1");
		assertThat(value.getPropertySource()).isEqualTo("polaris:default/order-service/application.yaml");
	}

	@Test
	void testConfigurationPropertiesFacadeSourceSkipped() {
		// Spring Boot 的 configurationProperties facade source 挂在链头,
		// 其 containsProperty 委托给所有底层 source(任意 key 都命中),
		// 不跳过会遮蔽真实来源,property_source 恒为 "configurationProperties"
		addFileSource(NAMESPACE, GROUP, FILE_NAME, Collections.singletonMap("k", "1"));
		environment.getPropertySources().addFirst(new MapPropertySource("configurationProperties",
				Collections.singletonMap("k", "1")));

		EffectiveValue value = provider.resolve("k", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		assertThat(value.getPropertySource()).isEqualTo("polaris:default/order-service/application.yaml");
	}

	@Test
	void testKeyNotPresentAnywhere() {
		addFileSource(NAMESPACE, GROUP, FILE_NAME, Collections.singletonMap("other.key", "1"));

		EffectiveValue value = provider.resolve("missing.key", metadata(NAMESPACE, GROUP, FILE_NAME));

		assertThat(value).isNotNull();
		assertThat(value.getFileValue()).isNull();
		assertThat(value.getEffectiveValue()).isNull();
		assertThat(value.getPropertySource()).isNull();
	}
}
