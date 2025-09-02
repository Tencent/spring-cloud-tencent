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

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
	public void testRunAsync() {
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
}
