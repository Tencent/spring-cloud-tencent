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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tencent.cloud.rpc.enhancement.plugin.EnhancedContextKeys;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;

/**
 * Helper that builds a W3C baggage HTTP header value from a map of attributes.
 *
 * <p>The previous approach reflectively called {@code Baggage.makeCurrent()} to attach baggage
 * to the current thread's OTel Context and closed it after the downstream request was sent. In
 * Reactor / Netty async scenarios, attach and close are not guaranteed to run on the same thread,
 * and the OTel Agent also restores the ThreadLocal when its own Scope closes. Both cause the
 * {@code current() != toAttach} check inside {@code ScopeImpl.close()} to fail, so the scope is
 * never actually released.</p>
 *
 * <p>To avoid this entirely, this helper does not touch {@code ThreadLocalContextStorage} at all.
 * Instead it produces a W3C baggage compliant header value. Callers write it into the downstream
 * request headers before sending; the OTel Agent on the server side parses it back into
 * {@code Baggage}, which is semantically identical to attach but has no ThreadLocal lifecycle.</p>
 *
 * @author Haotian Zhang
 */
public final class OtelBaggageScopeHelper {

	/**
	 * Standard W3C baggage HTTP header name.
	 */
	public static final String BAGGAGE_HEADER = "baggage";

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	/**
	 * The {@code baggage-octet} set from the W3C Baggage spec: US-ASCII characters excluding CTLs,
	 * whitespace, DQUOTE, comma, semicolon and backslash. Any code point outside this set must be
	 * percent-encoded. This intentionally differs from {@code x-www-form-urlencoded}: a space becomes
	 * {@code %20}, not {@code +}, because OTel's {@code BaggageCodec} only decodes {@code %XX} and
	 * keeps {@code +} as a literal plus.
	 */
	private static final BitSet BAGGAGE_OCTET = new BitSet(128);

	static {
		for (int c = 0x21; c <= 0x7E; c++) {
			BAGGAGE_OCTET.set(c);
		}
		BAGGAGE_OCTET.clear('"');
		BAGGAGE_OCTET.clear(',');
		BAGGAGE_OCTET.clear(';');
		BAGGAGE_OCTET.clear('\\');
	}

	private OtelBaggageScopeHelper() {
	}

	/**
	 * Encode the given attributes as a W3C baggage header value.
	 *
	 * <p>Format is {@code key1=value1,key2=value2}; both key and value are percent-encoded per
	 * RFC 3986. Returns {@code null} when the input is empty.</p>
	 *
	 * @param attributes attributes to encode
	 * @return W3C baggage header value or null when input is empty
	 */
	public static String encodeBaggage(Map<String, String> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : attributes.entrySet()) {
			if (e.getKey() == null || e.getValue() == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(percentEncode(e.getKey())).append('=').append(percentEncode(e.getValue()));
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	/**
	 * Merge the given attributes into an existing baggage header value.
	 *
	 * <p>Existing keys are overwritten by the new value; other keys are preserved. Returns the
	 * input unchanged when no attributes are provided.</p>
	 *
	 * @param existing existing baggage header value (nullable)
	 * @param attributes attributes to merge in
	 * @return merged baggage header value, or null when both inputs empty
	 */
	public static String mergeIntoBaggageHeader(String existing, Map<String, String> attributes) {
		if (existing == null || existing.isEmpty()) {
			return encodeBaggage(attributes);
		}
		Map<String, String> merged = new LinkedHashMap<>();
		for (String pair : existing.split(",")) {
			String trimmed = pair.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int semi = trimmed.indexOf(';');
			String kv = semi >= 0 ? trimmed.substring(0, semi) : trimmed;
			int eq = kv.indexOf('=');
			if (eq <= 0) {
				continue;
			}
			merged.put(percentDecode(kv.substring(0, eq).trim()), percentDecode(kv.substring(eq + 1).trim()));
		}
		if (attributes != null) {
			for (Map.Entry<String, String> e : attributes.entrySet()) {
				if (e.getKey() != null && e.getValue() != null) {
					merged.put(e.getKey(), e.getValue());
				}
			}
		}
		return encodeBaggage(merged);
	}

	/**
	 * Resolve the pending baggage staged in the context into a baggage header value to write on the
	 * downstream request. Removes {@link EnhancedContextKeys#PENDING_BAGGAGE_ATTRIBUTES_KEY} from the
	 * context and merges it with the request's existing baggage header. Returns {@code null} when
	 * there is nothing to write, so callers can leave the request untouched.
	 *
	 * @param ctx enhanced plugin context holding the staged baggage attributes
	 * @param existingHeader the request's current baggage header value (nullable)
	 * @return the merged baggage header value, or null when there is nothing to write
	 */
	@SuppressWarnings("unchecked")
	public static String resolvePendingBaggageHeader(EnhancedPluginContext ctx, String existingHeader) {
		Map<String, String> pending = (Map<String, String>) ctx.getExtraData()
				.remove(EnhancedContextKeys.PENDING_BAGGAGE_ATTRIBUTES_KEY);
		if (pending == null || pending.isEmpty()) {
			return null;
		}
		return mergeIntoBaggageHeader(existingHeader, pending);
	}

	private static String percentEncode(String s) {
		// Common case: plain ASCII keys/values (trace ids, hex) are all baggage-octet, so return the
		// input without touching the byte[] / StringBuilder path.
		if (needsNoEncoding(s)) {
			return s;
		}
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		StringBuilder sb = new StringBuilder(bytes.length);
		for (byte value : bytes) {
			int b = value & 0xFF;
			if (b < 128 && BAGGAGE_OCTET.get(b)) {
				sb.append((char) b);
			}
			else {
				sb.append('%').append(HEX[b >> 4]).append(HEX[b & 0x0F]);
			}
		}
		return sb.toString();
	}

	private static boolean needsNoEncoding(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 128 || !BAGGAGE_OCTET.get(c)) {
				return false;
			}
		}
		return true;
	}

	private static String percentDecode(String s) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '%' && i + 2 < s.length()) {
				int high = Character.digit(s.charAt(i + 1), 16);
				int low = Character.digit(s.charAt(i + 2), 16);
				if (high >= 0 && low >= 0) {
					buffer.write((high << 4) + low);
					i += 2;
					continue;
				}
			}
			buffer.write(c);
		}
		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}
}
