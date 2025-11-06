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

package com.tencent.cloud.common.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.concurrent.FutureUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link PolarisCompletableFutureUtils}.
 *
 * @author Haotian Zhang
 */
public class PolarisCompletableFutureUtilsTest {

	@BeforeEach
	void setUp() {
		MetadataContext metadataContext = MetadataContextHolder.get();
		metadataContext.putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1", "value1");
	}

	@AfterEach
	void tearDown() {
		MetadataContextHolder.remove();
	}

	@Test
	public void testSupplyAsync() {
		// can not be found and set
		CompletableFuture.supplyAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isNull();
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value2");
			return "test";
		}).thenAccept(result -> {
			assertThat(result).isEqualTo("test");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();

		// can be found and set
		PolarisCompletableFutureUtils.supplyAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value2");
			return "test";
		}).thenAccept(result -> {
			assertThat(result).isEqualTo("test");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isEqualTo("value2");
	}

	@Test
	public void testSupplyAsyncWithNullSupplier() {
		assertThatThrownBy(() -> PolarisCompletableFutureUtils.supplyAsync(null))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Supplier must not be null");
	}

	@Test
	public void testRunAsync() {
		// can not be found and set
		CompletableFuture.runAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isNull();
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value3");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();

		// can be found and set
		PolarisCompletableFutureUtils.runAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value3");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isEqualTo("value3");
	}

	@Test
	public void testRunAsyncWithNullRunnable() {
		assertThatThrownBy(() -> PolarisCompletableFutureUtils.runAsync(null))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Runnable must not be null");
	}

	@Test
	public void testCallAsync() {
		// can not be found and set
		FutureUtils.callAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isNull();
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value2");
			return "test";
		}).thenAccept(result -> {
			assertThat(result).isEqualTo("test");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();

		// can be found and set
		PolarisCompletableFutureUtils.callAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value2");
			return "test";
		}).thenAccept(result -> {
			assertThat(result).isEqualTo("test");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isEqualTo("value2");
	}

	@Test
	public void testCallAsyncWithNullCallable() {
		assertThatThrownBy(() -> PolarisCompletableFutureUtils.callAsync(null))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Callable must not be null");
	}

	@Test
	public void testCallAsyncWithExecutor() {
		Executor executor = Executors.newSingleThreadExecutor();

		// can be found and set
		PolarisCompletableFutureUtils.callAsync(() -> {
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
			assertThat(MetadataContextHolder.get()
					.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isNull();
			MetadataContextHolder.get().putContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2", "value2");
			return "test";
		}, executor).thenAccept(result -> {
			assertThat(result).isEqualTo("test");
		}).join();
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key1")).isEqualTo("value1");
		assertThat(MetadataContextHolder.get()
				.getContext(MetadataContext.FRAGMENT_TRANSITIVE, "key2")).isEqualTo("value2");
	}

	@Test
	public void testCallAsyncWithNullExecutor() {
		assertThatThrownBy(() -> PolarisCompletableFutureUtils.callAsync(null, null))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Callable must not be null");

		assertThatThrownBy(() -> PolarisCompletableFutureUtils.callAsync(() -> "test", null))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Executor must not be null");
	}
}
