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

package com.tencent.cloud.metadata.concurrent;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link MetadataRunnable}.
 *
 * @author wlx
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = MetadataRunnableTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false"})
public class MetadataRunnableTest {

	private static final ExecutorService executor = Executors.newFixedThreadPool(1);

	@Test
	public void threadMultiplexingTest() {
		Map<String, String> fragmentContextBeforeInit =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		executor.submit(() -> {
			Map<String, String> fragmentContext =
					MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
			Assertions.assertThat(fragmentContextBeforeInit.equals(fragmentContext)).isTrue();
		});

		// init after new Task, won't see parent value in in task!
		MetadataTestUtil.initMetadataContext();

		Map<String, String> fragmentContextAfterInit =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);


		executor.submit(() -> {
			Map<String, String> fragmentContext =
					MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
			// init after new Task, won't see parent value in in task!
			// so before init and after init task res will be same!
			Assertions.assertThat(fragmentContextBeforeInit.equals(fragmentContext)).isTrue();
		});

		Assertions.assertThat(fragmentContextAfterInit.get("a")).isEqualTo("1");
		Assertions.assertThat(fragmentContextAfterInit.get("b")).isEqualTo("2");

	}

	@Test
	public void metadataRunnableTest() {
		Map<String, String> fragmentContextBeforeInit =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		executor.submit(MetadataRunnable.get(
				() -> {
					Map<String, String> fragmentContext =
							MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
					Assertions.assertThat(fragmentContextBeforeInit.equals(fragmentContext));
				}
		));

		MetadataTestUtil.initMetadataContext();

		Map<String, String> fragmentContextAfterInit =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		executor.submit(MetadataRunnable.get(
				() -> {
					Map<String, String> fragmentContext =
							MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
					Assertions.assertThat(fragmentContextBeforeInit.equals(fragmentContext)).isFalse();
					Assertions.assertThat(fragmentContextAfterInit.equals(fragmentContext)).isTrue();
				}
		));

		Assertions.assertThat(fragmentContextAfterInit.get("a")).isEqualTo("1");
		Assertions.assertThat(fragmentContextAfterInit.get("b")).isEqualTo("2");
	}

	@Test
	public void metadataRunnableWrapTest() {
		Runnable runnable = MetadataRunnable.get(
				() -> {
					MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
				}
		);
		Assertions.assertThat(runnable instanceof MetadataRunnable).isTrue();
	}

	@AfterClass
	public static void cleanUp() {
		executor.shutdownNow();
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
