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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.cloud.polaris.config.config.PolarisConfigProperties;
import com.tencent.cloud.polaris.config.spring.property.PlaceholderHelper;
import com.tencent.cloud.polaris.config.spring.property.SpringValue;
import com.tencent.cloud.polaris.config.spring.property.SpringValueRegistry;
import com.tencent.cloud.polaris.config.utils.PolarisPropertySourceUtils;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ChangeType;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFileChangeEvent;
import com.tencent.polaris.configuration.api.core.ConfigPropertyChangeInfo;
import com.tencent.polaris.configuration.client.internal.CompositeConfigFile;
import com.tencent.polaris.configuration.client.internal.RevisableConfigFileGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for sensitive data masking of encrypted config files.
 *
 * <p>Covers the log-masking behaviour of {@link PolarisConfigPropertyAutoRefresher},
 * {@link PolarisRefreshAffectedContextRefresher} and {@link PolarisPropertySourceUtils}.
 *
 * @author evelynwei
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisConfigSensitiveDataMaskingTest {

	private static final String SENSITIVE_VALUE = "root-password-1234";

	private static final String PLAIN_VALUE = "plain-value";

	private final String testNamespace = "testNamespace";

	private final String testFileGroup = "testFileGroup";

	private final String testFileName = "application.properties";

	@Mock
	private PolarisConfigProperties polarisConfigProperties;

	@Mock
	private SpringValueRegistry springValueRegistry;

	@Mock
	private PlaceholderHelper placeholderHelper;

	@Mock
	private ConfigFileService configFileService;

	@Mock
	private ContextRefresher contextRefresher;

	@Mock
	private SDKContext sdkContext;

	@Mock
	private Extensions extensions;

	@Mock
	private ValueContext valueContext;

	private final List<ListAppender<ILoggingEvent>> appenders = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		PolarisPropertySourceManager.clearPropertySources();
		PolarisConfigPropertyAutoRefresher.clearEncryptedPropertyKeys();
	}

	@AfterEach
	public void tearDown() {
		PolarisConfigPropertyAutoRefresher.clearEncryptedPropertyKeys();
		for (ListAppender<ILoggingEvent> appender : appenders) {
			appender.stop();
		}
		appenders.clear();
	}

	/**
	 * Item 5: maskValue keeps the length and fingerprint hints and never exposes the content.
	 */
	@Test
	public void testMaskValue() {
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue(null)).isNull();
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue("")).isEmpty();
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue(SENSITIVE_VALUE))
				.matches("\\*\\*\\*\\(len=" + SENSITIVE_VALUE.length() + ", fp=[0-9a-f]{8}\\)")
				.doesNotContain(SENSITIVE_VALUE);
		// non-String Object must not blow up
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue(12345)).matches("\\*\\*\\*\\(len=5, fp=[0-9a-f]{8}\\)");
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue(Boolean.TRUE))
				.matches("\\*\\*\\*\\(len=4, fp=[0-9a-f]{8}\\)");
	}

	/**
	 * Item 10: the fingerprint is stable for the same value and differs for different values of the
	 * same length. This is what makes an old/new pair distinguishable when their lengths match.
	 */
	@Test
	public void testFingerprintDistinguishesEqualLengthValues() {
		String oldValue = "passwordAAAA";
		String newValue = "passwordBBBB";
		assertThat(oldValue).hasSameSizeAs(newValue);

		String maskedOld = PolarisConfigPropertyAutoRefresher.maskValue(oldValue);
		String maskedNew = PolarisConfigPropertyAutoRefresher.maskValue(newValue);

		assertThat(maskedOld).isNotEqualTo(maskedNew);
		// stable within the same process, so repeated logging of one value reads consistently
		assertThat(PolarisConfigPropertyAutoRefresher.maskValue(oldValue)).isEqualTo(maskedOld);
	}

	/**
	 * Item 3: keys of an encrypted file are registered, keys of a plain file are not.
	 */
	@Test
	public void testEncryptedKeysRegisteredOnlyForEncryptedFile() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();

		MockedConfigKVFile encryptedFile = new MockedConfigKVFile(contentOf("encrypted.key", SENSITIVE_VALUE));
		fireChange(refresher, encryptedFile, encryptedConfigFile(), "encrypted.key",
				new ConfigPropertyChangeInfo("encrypted.key", "old", SENSITIVE_VALUE, ChangeType.MODIFIED));
		assertThat(refresher.isEncryptedKey("encrypted.key")).isTrue();

		MockedConfigKVFile plainFile = new MockedConfigKVFile(contentOf("plain.key", PLAIN_VALUE));
		fireChange(refresher, plainFile, plainConfigFile(), "plain.key",
				new ConfigPropertyChangeInfo("plain.key", "old", PLAIN_VALUE, ChangeType.MODIFIED));
		assertThat(refresher.isEncryptedKey("plain.key")).isFalse();
	}

	/**
	 * Item 4: a null ConfigFile (ConfigKVFileChangeEvent#getConfigFile may be null) must neither
	 * throw nor register anything.
	 */
	@Test
	public void testNullConfigFileNeitherThrowsNorRegisters() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();

		MockedConfigKVFile file = new MockedConfigKVFile(contentOf("some.key", PLAIN_VALUE));
		fireChange(refresher, file, null, "some.key",
				new ConfigPropertyChangeInfo("some.key", "old", PLAIN_VALUE, ChangeType.MODIFIED));

		assertThat(refresher.isEncryptedKey("some.key")).isFalse();
	}

	/**
	 * Item 1: the change log of an encrypted config file carries no raw value.
	 */
	@Test
	public void testEncryptedConfigChangeLogIsMasked() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisConfigPropertyAutoRefresher.class);

		MockedConfigKVFile file = new MockedConfigKVFile(contentOf("db.password", SENSITIVE_VALUE));
		fireChange(refresher, file, encryptedConfigFile(), "db.password",
				new ConfigPropertyChangeInfo("db.password", SENSITIVE_VALUE, SENSITIVE_VALUE + "-new",
						ChangeType.MODIFIED));

		String logs = renderLogs(appender);
		assertThat(logs).doesNotContain(SENSITIVE_VALUE);
		// key and change type stay observable for troubleshooting
		assertThat(logs).contains("db.password").contains("MODIFIED").contains("***(len=");
	}

	/**
	 * Item 2: a plain config file keeps the original behaviour, i.e. the SDK's own
	 * ConfigPropertyChangeInfo rendering, with no masking applied by us.
	 * <p>
	 * That rendering carries the key and the change type but no values, so this asserts the
	 * absence of the mask marker rather than the presence of the raw value.
	 */
	@Test
	public void testPlainConfigChangeLogIsNotMasked() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisConfigPropertyAutoRefresher.class);

		MockedConfigKVFile file = new MockedConfigKVFile(contentOf("app.name", PLAIN_VALUE));
		fireChange(refresher, file, plainConfigFile(), "app.name",
				new ConfigPropertyChangeInfo("app.name", "old-name", PLAIN_VALUE, ChangeType.MODIFIED));

		String logs = renderLogs(appender);
		assertThat(logs).contains("app.name").contains("MODIFIED").doesNotContain("***(len=");
	}

	/**
	 * Item 6: updateSpringValue masks the new value when the changed key is encrypted, and keeps it
	 * as is otherwise.
	 */
	@Test
	public void testSpringValueRefreshLogRespectsEncryptedKey() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = buildAffectedRefresher(SENSITIVE_VALUE);
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisRefreshAffectedContextRefresher.class);

		MockedConfigKVFile encryptedFile = new MockedConfigKVFile(contentOf("db.password", SENSITIVE_VALUE));
		fireChange(refresher, encryptedFile, encryptedConfigFile(), "db.password",
				new ConfigPropertyChangeInfo("db.password", "old", SENSITIVE_VALUE, ChangeType.MODIFIED));

		String logs = renderLogs(appender);
		assertThat(logs).contains("Auto update polaris changed value successfully");
		assertThat(logs).doesNotContain(SENSITIVE_VALUE);
		assertThat(logs).contains("***(len=" + SENSITIVE_VALUE.length() + ", fp=");
	}

	@Test
	public void testSpringValueRefreshLogKeepsPlainValue() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = buildAffectedRefresher(PLAIN_VALUE);
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisRefreshAffectedContextRefresher.class);

		MockedConfigKVFile plainFile = new MockedConfigKVFile(contentOf("app.name", PLAIN_VALUE));
		fireChange(refresher, plainFile, plainConfigFile(), "app.name",
				new ConfigPropertyChangeInfo("app.name", "old", PLAIN_VALUE, ChangeType.MODIFIED));

		String logs = renderLogs(appender);
		assertThat(logs).contains(PLAIN_VALUE).doesNotContain("***(len=");
	}

	/**
	 * Item 9: SpringValue#toString carries no property value, which is why the refresh failure log
	 * needs no masking.
	 */
	@Test
	public void testSpringValueToStringCarriesNoValue() throws Exception {
		MockedConfigChange bean = new MockedConfigChange();
		bean.setK1(SENSITIVE_VALUE);
		Field field = bean.getClass().getDeclaredField("k1");
		SpringValue springValue = new SpringValue("db.password", "${db.password}", bean, "mockedConfigChange", field);

		assertThat(springValue.toString())
				.doesNotContain(SENSITIVE_VALUE)
				.contains("db.password")
				.contains("mockedConfigChange");
	}

	/**
	 * Item 7: a group holding an encrypted file logs key names only, never values.
	 */
	@Test
	public void testGroupPropertySourceDebugLogCarriesNoValueWhenEncrypted() {
		String logs = loadGroupAndRenderDebugLogs("db.password", SENSITIVE_VALUE, true);

		assertThat(logs).doesNotContain(SENSITIVE_VALUE);
		assertThat(logs).contains("db.password").contains("propertyCount = 1").contains("values omitted");
	}

	/**
	 * A group with no encrypted file keeps the original behaviour and logs the merged map, so the
	 * unencrypted scenario loses no diagnosability.
	 */
	@Test
	public void testGroupPropertySourceDebugLogKeepsMapWhenNotEncrypted() {
		String logs = loadGroupAndRenderDebugLogs("app.name", PLAIN_VALUE, false);

		assertThat(logs).contains("app.name").contains(PLAIN_VALUE).contains("map = ");
	}

	/**
	 * Loads a one-file group at DEBUG level and returns what the loader logged.
	 */
	private String loadGroupAndRenderDebugLogs(String key, String value, boolean encrypted) {
		ch.qos.logback.classic.Logger logger =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger(PolarisPropertySourceUtils.class);
		Level originalLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisPropertySourceUtils.class);
		try {
			MockedConfigKVFile file = new MockedConfigKVFile(contentOf(key, value),
					testFileName, testFileGroup, testNamespace);
			file.setEncrypted(encrypted);
			com.tencent.polaris.configuration.api.core.ConfigFileGroup group =
					new com.tencent.polaris.configuration.client.internal.RevisableConfigFileGroup(
							testNamespace, testFileGroup, java.util.Collections.singletonList(file), "v1");
			when(configFileService.getConfigFileGroup(testNamespace, testFileGroup)).thenReturn(group);
			when(configFileService.getConfigPropertiesFile(testNamespace, testFileGroup, testFileName))
					.thenReturn(file);

			PolarisPropertySource source = PolarisPropertySourceUtils
					.loadGroupPolarisPropertySource(configFileService, testNamespace, testFileGroup);

			assertThat(source).isNotNull();
			return renderLogs(appender);
		}
		finally {
			logger.setLevel(originalLevel);
		}
	}

	/**
	 * Item 8: locks the known boundary of the grow-only key set. A key that has never been seen in
	 * a file-dimension change event is not masked on its first refresh; once registered it stays
	 * masked even when the very same key later arrives from a plain file.
	 */
	@Test
	public void testGrowOnlyKeySetBoundary() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();

		// never seen before -> not masked
		assertThat(refresher.isEncryptedKey("shared.key")).isFalse();

		MockedConfigKVFile encryptedFile = new MockedConfigKVFile(contentOf("shared.key", SENSITIVE_VALUE));
		fireChange(refresher, encryptedFile, encryptedConfigFile(), "shared.key",
				new ConfigPropertyChangeInfo("shared.key", "old", SENSITIVE_VALUE, ChangeType.MODIFIED));
		assertThat(refresher.isEncryptedKey("shared.key")).isTrue();

		// a later plain file carrying the same key does not un-register it
		MockedConfigKVFile plainFile = new MockedConfigKVFile(contentOf("shared.key", PLAIN_VALUE));
		fireChange(refresher, plainFile, plainConfigFile(), "shared.key",
				new ConfigPropertyChangeInfo("shared.key", SENSITIVE_VALUE, PLAIN_VALUE, ChangeType.MODIFIED));
		assertThat(refresher.isEncryptedKey("shared.key")).isTrue();
	}

	/**
	 * An ADDED key may be absent from {@code ConfigKVFile#getPropertyNames()} when the listener
	 * runs; it must still be registered from {@code changedKeys()}.
	 */
	@Test
	public void testAddedEncryptedKeyIsRegisteredFromChangeEvent() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisConfigPropertyAutoRefresher.class);

		MockedConfigKVFile file = new MockedConfigKVFile(contentOf("existing.key", PLAIN_VALUE));
		fireChange(refresher, file, encryptedConfigFile(), "db.password",
				new ConfigPropertyChangeInfo("db.password", null, SENSITIVE_VALUE, ChangeType.ADDED));

		assertThat(refresher.isEncryptedKey("db.password")).isTrue();
		assertThat(renderLogs(appender)).doesNotContain(SENSITIVE_VALUE).contains("***(len=");
	}

	/**
	 * A log level is not a secret, so the logging.level line keeps the raw value even when the
	 * file is encrypted. Encryption is a per-file flag, so it also covers harmless keys.
	 */
	@Test
	public void testEncryptedLoggingLevelChangeKeepsRawValue() {
		PolarisConfigPropertyAutoRefresher refresher = buildRefresher();
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisConfigPropertyAutoRefresher.class);

		String levelKey = "logging.level.com.example";
		MockedConfigKVFile file = new MockedConfigKVFile(contentOf(levelKey, "DEBUG"));
		fireChange(refresher, file, encryptedConfigFile(), levelKey,
				new ConfigPropertyChangeInfo(levelKey, "INFO", "DEBUG", ChangeType.MODIFIED));

		assertThat(renderLogs(appender)).contains("set logging.level loggerName:com.example, newValue:DEBUG");
	}

	/**
	 * A file newly added to a watched group carries its own encrypted flag, so its keys are
	 * registered at load time and the first @Value refresh log is already masked.
	 */
	@Test
	public void testGroupAddOfEncryptedFileMasksSpringValueRefreshLog() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = buildAffectedRefresher(SENSITIVE_VALUE);
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisRefreshAffectedContextRefresher.class);

		fireGroupAdd(refresher, "encrypted-add.properties", SENSITIVE_VALUE, true);

		assertThat(refresher.isEncryptedKey("db.password")).isTrue();
		assertThat(renderLogs(appender)).contains("Auto update polaris changed value successfully")
				.doesNotContain(SENSITIVE_VALUE)
				.contains("***(len=");
	}

	/**
	 * A plain file added to a watched group keeps the original behaviour and logs the raw value:
	 * judging per file means the unencrypted scenario loses no diagnosability.
	 */
	@Test
	public void testGroupAddOfPlainFileKeepsRawValue() throws Exception {
		PolarisRefreshAffectedContextRefresher refresher = buildAffectedRefresher(PLAIN_VALUE);
		ListAppender<ILoggingEvent> appender = attachAppender(PolarisRefreshAffectedContextRefresher.class);

		fireGroupAdd(refresher, "plain-add.properties", PLAIN_VALUE, false);

		assertThat(refresher.isEncryptedKey("db.password")).isFalse();
		assertThat(renderLogs(appender)).contains(PLAIN_VALUE).doesNotContain("***(len=");
	}

	/**
	 * Registers a one-file group, then adds {@code addedFileName} carrying {@code addedValue}
	 * under {@code db.password} and waits for the group-add refresh to land.
	 * <p>
	 * Each caller must pass a distinct {@code addedFileName}: the registered-property-source set
	 * behind the group listener is static and grow-only, so a reused name is silently skipped.
	 */
	private void fireGroupAdd(PolarisRefreshAffectedContextRefresher refresher, String addedFileName,
			String addedValue, boolean addedFileEncrypted) throws InterruptedException {
		Map<String, Object> existing = new ConcurrentHashMap<>();
		existing.put("app.name", PLAIN_VALUE);
		MockedConfigKVFile file = new MockedConfigKVFile(existing, testFileName, testFileGroup, testNamespace);
		when(configFileService.getConfigPropertiesFile(testNamespace, testFileGroup, testFileName))
				.thenReturn(file);

		CompositeConfigFile compositeConfigFile = new CompositeConfigFile(Collections.singletonList(file));
		PolarisPropertySource polarisPropertySource = new PolarisPropertySource(testNamespace, testFileGroup,
				testFileName, compositeConfigFile, new ConcurrentHashMap<>(existing));
		PolarisPropertySourceManager.addPropertySource(polarisPropertySource);

		RevisableConfigFileGroup group = new RevisableConfigFileGroup(testNamespace, testFileGroup,
				Collections.singletonList(file), "v1");
		when(configFileService.getConfigFileGroup(testNamespace, testFileGroup)).thenReturn(group);
		when(sdkContext.getExtensions()).thenReturn(extensions);
		when(extensions.getValueContext()).thenReturn(valueContext);

		refresher.onApplicationEvent(null);

		Map<String, Object> added = new ConcurrentHashMap<>();
		added.put("db.password", addedValue);
		MockedConfigKVFile file2 = new MockedConfigKVFile(added, addedFileName, testFileGroup, testNamespace);
		file2.setEncrypted(addedFileEncrypted);
		when(configFileService.getConfigPropertiesFile(testNamespace, testFileGroup, addedFileName))
				.thenReturn(file2);

		group.updateConfigFileList(Arrays.asList(file, file2), "v2");

		long deadline = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < deadline && polarisPropertySource.getProperty("db.password") == null) {
			Thread.sleep(50);
		}
		assertThat(polarisPropertySource.getProperty("db.password")).isEqualTo(addedValue);
	}

	/**
	 * The affected-context refresher is used throughout: it is the default implementation and the
	 * only one whose {@code refreshConfigurationProperties} works against a mocked context.
	 */
	private PolarisConfigPropertyAutoRefresher buildRefresher() {
		try {
			return buildAffectedRefresher(PLAIN_VALUE);
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to build refresher", e);
		}
	}

	private PolarisRefreshAffectedContextRefresher buildAffectedRefresher(String resolvedValue) throws Exception {
		when(polarisConfigProperties.isAutoRefresh()).thenReturn(true);
		when(sdkContext.getExtensions()).thenReturn(extensions);
		when(extensions.getValueContext()).thenReturn(valueContext);
		when(valueContext.getClientId()).thenReturn("mockClientId");
		when(valueContext.getHost()).thenReturn("mockHost");

		PolarisRefreshAffectedContextRefresher refresher = new PolarisRefreshAffectedContextRefresher(
				polarisConfigProperties, springValueRegistry, placeholderHelper, configFileService,
				contextRefresher, sdkContext);

		ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);
		ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
		TypeConverter typeConverter = mock(TypeConverter.class);
		when(beanFactory.getTypeConverter()).thenReturn(typeConverter);
		when(applicationContext.getBeanFactory()).thenReturn(beanFactory);
		refresher.setApplicationContext(applicationContext);
		when(typeConverter.convertIfNecessary(any(), any(), (Field) any())).thenReturn(resolvedValue);

		MockedConfigChange bean = new MockedConfigChange();
		Field field = bean.getClass().getDeclaredField("k1");
		Collection<SpringValue> springValues = new ArrayList<>();
		springValues.add(new SpringValue("db.password", "${db.password}", bean, "mockedConfigChange", field));
		when(springValueRegistry.get(any(), any())).thenReturn(springValues);

		return refresher;
	}

	/**
	 * Registers the property source, wires the change listener and fires one change event.
	 */
	private void fireChange(PolarisConfigPropertyAutoRefresher refresher, MockedConfigKVFile file,
			ConfigFile configFile, String changedKey, ConfigPropertyChangeInfo changeInfo) {
		Map<String, Object> source = new HashMap<>(contentOf(changedKey, changeInfo.getOldValue()));
		PolarisPropertySource propertySource = new PolarisPropertySource(file.getNamespace(), file.getFileGroup(),
				file.getFileName(), file, source);
		refresher.registerPolarisConfigPublishChangeListener(propertySource);

		Map<String, ConfigPropertyChangeInfo> changeInfos = new HashMap<>();
		changeInfos.put(changedKey, changeInfo);
		file.fireChangeListener(new ConfigKVFileChangeEvent(changeInfos, configFile));
	}

	private ConfigFile encryptedConfigFile() {
		ConfigFile configFile = new ConfigFile(testNamespace, testFileGroup, testFileName);
		configFile.setEncrypted(true);
		return configFile;
	}

	private ConfigFile plainConfigFile() {
		ConfigFile configFile = new ConfigFile(testNamespace, testFileGroup, testFileName);
		configFile.setEncrypted(false);
		return configFile;
	}

	private Map<String, Object> contentOf(String key, Object value) {
		Map<String, Object> content = new HashMap<>();
		content.put(key, value);
		return content;
	}

	private ListAppender<ILoggingEvent> attachAppender(Class<?> loggerClass) {
		ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerClass);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.setContext(logger.getLoggerContext());
		appender.start();
		logger.addAppender(appender);
		appenders.add(appender);
		return appender;
	}

	private String renderLogs(ListAppender<ILoggingEvent> appender) {
		StringBuilder builder = new StringBuilder();
		for (ILoggingEvent event : appender.list) {
			builder.append(event.getFormattedMessage()).append('\n');
		}
		return builder.toString();
	}
}
