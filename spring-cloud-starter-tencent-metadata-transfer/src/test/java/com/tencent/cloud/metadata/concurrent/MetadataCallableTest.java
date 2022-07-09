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

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link MetadataCallable}.
 *
 * @author wlx
 * @date 2022/7/9 12:11 下午
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = MetadataCallableTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false"})
public class MetadataCallableTest {

	private static final ExecutorService executor = Executors.newFixedThreadPool(1);

	@Test
	public void threadMultiplexingTest() throws InterruptedException, ExecutionException, TimeoutException {

		Future<Map<String, String>> future = executor.submit(() -> {
			return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		});
		Map<String, String> map = future.get(200, TimeUnit.MILLISECONDS);

		// init after new Task, won't see parent value in in task!
		MetadataTestUtil.initMetadataContext();

		Future<Map<String, String>> future1 = executor.submit(() -> {
			return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		});

		Map<String, String> map1 = future1.get(200, TimeUnit.MILLISECONDS);

		Map<String, String> fragmentContext =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		Assertions.assertThat(fragmentContext.get("a")).isEqualTo("1");
		Assertions.assertThat(fragmentContext.get("b")).isEqualTo("2");

		// init after new Task, won't see parent value in in task!,so before init and after init task res will be same!
		Assertions.assertThat(map.equals(map1)).isTrue();
	}

	@Test
	public void metadataCallableTest() throws InterruptedException, ExecutionException, TimeoutException {
		Future<Map<String, String>> future = executor.submit(
				MetadataCallable.get(
						() -> MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
				)
		);
		Map<String, String> map = future.get(200, TimeUnit.MILLISECONDS);

		Assertions.assertThat(map.equals(MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)));

		MetadataTestUtil.initMetadataContext();

		Future<Map<String, String>> future1 = executor.submit(
				MetadataCallable.get(
						() -> MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
				)
		);

		Map<String, String> map1 = future1.get(200, TimeUnit.MILLISECONDS);

		Map<String, String> fragmentContext =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		Assertions.assertThat(fragmentContext.get("a")).isEqualTo("1");
		Assertions.assertThat(fragmentContext.get("b")).isEqualTo("2");

		Assertions.assertThat(fragmentContext.equals(map1)).isTrue();
	}

	@Test
	public void metadataCallableWrap() {
		Callable<Map<String, String>> callable = MetadataCallable.get(
				() -> MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
		);
		Assertions.assertThat(callable instanceof MetadataCallable).isTrue();
	}

	@AfterClass
	public static void cleanUp() {
		executor.shutdownNow();
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
