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

package com.tencent.cloud.rpc.enhancement.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OtelBaggageScopeHelper}.
 */
public class OtelBaggageScopeHelperTest {

	@Test
	public void testEncodeBaggage_empty() {
		assertThat(OtelBaggageScopeHelper.encodeBaggage(null)).isNull();
		assertThat(OtelBaggageScopeHelper.encodeBaggage(new HashMap<>())).isNull();
	}

	@Test
	public void testEncodeBaggage_simple() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k1", "v1");
		attrs.put("k2", "v2");

		String header = OtelBaggageScopeHelper.encodeBaggage(attrs);
		assertThat(header).isEqualTo("k1=v1,k2=v2");
	}

	@Test
	public void testEncodeBaggage_specialChars() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("custom.k", "v with space");
		attrs.put("namespace-id", "ns-a,b");

		String header = OtelBaggageScopeHelper.encodeBaggage(attrs);
		// space -> %20 (RFC 3986, not x-www-form-urlencoded '+'), comma -> %2C, dot kept as-is
		assertThat(header).contains("custom.k=v%20with%20space");
		assertThat(header).contains("namespace-id=ns-a%2Cb");
	}

	@Test
	public void testEncodeBaggage_skipNullEntry() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k1", "v1");
		attrs.put("k2", null);
		attrs.put(null, "v3");
		attrs.put("k4", "v4");

		String header = OtelBaggageScopeHelper.encodeBaggage(attrs);
		assertThat(header).isEqualTo("k1=v1,k4=v4");
	}

	@Test
	public void testEncodeBaggage_spaceUsesPercent20NotPlus() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k", "a b");

		String header = OtelBaggageScopeHelper.encodeBaggage(attrs);
		// OTel BaggageCodec decodes only %XX and keeps '+' literal, so a space must be %20, never '+'.
		assertThat(header).isEqualTo("k=a%20b");
		assertThat(header).doesNotContain("+");
	}

	@Test
	public void testMergeIntoBaggageHeader_percentEncodedExistingRoundTrips() {
		// An existing header carrying a percent-encoded space must be decoded, then re-encoded back to
		// %20 (not corrupted to '+') while a newly merged key is appended.
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k2", "v2");

		String merged = OtelBaggageScopeHelper.mergeIntoBaggageHeader("k1=a%20b", attrs);
		assertThat(merged).contains("k1=a%20b");
		assertThat(merged).contains("k2=v2");
		assertThat(merged).doesNotContain("+");
	}

	@Test
	public void testMergeIntoBaggageHeader_overwriteExistingKey() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k1", "new-v1");
		attrs.put("k3", "v3");

		String merged = OtelBaggageScopeHelper.mergeIntoBaggageHeader("k1=old-v1,k2=v2", attrs);
		// k1 overwritten with new-v1, k2 preserved, k3 appended
		assertThat(merged).contains("k1=new-v1");
		assertThat(merged).contains("k2=v2");
		assertThat(merged).contains("k3=v3");
		assertThat(merged).doesNotContain("old-v1");
	}

	@Test
	public void testMergeIntoBaggageHeader_existingWithMetadata() {
		// W3C baggage allows an optional ; property suffix per entry; parsing must drop the property
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k2", "v2");

		String merged = OtelBaggageScopeHelper.mergeIntoBaggageHeader("k1=v1;metadata=x", attrs);
		assertThat(merged).contains("k1=v1");
		assertThat(merged).contains("k2=v2");
		assertThat(merged).doesNotContain("metadata");
	}

	@Test
	public void testMergeIntoBaggageHeader_nullExisting() {
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("k1", "v1");

		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader(null, attrs)).isEqualTo("k1=v1");
		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader("", attrs)).isEqualTo("k1=v1");
	}

	@Test
	public void testMergeIntoBaggageHeader_emptyAttrs() {
		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader("k1=v1", null)).isEqualTo("k1=v1");
		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader("k1=v1", new HashMap<>())).isEqualTo("k1=v1");
	}

	@Test
	public void testMergeIntoBaggageHeader_bothEmpty() {
		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader(null, null)).isNull();
		assertThat(OtelBaggageScopeHelper.mergeIntoBaggageHeader("", new HashMap<>())).isNull();
	}
}
