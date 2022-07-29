package com.tencent.cloud.polaris.gateway.example.callee.xss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrap HttpServletRequest to escape String arguments
 *
 * @author Daifu Wu
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
	private byte[] requestBody;

	public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);
		BufferedReader reader = request.getReader();
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
		}
		if (stringBuilder.length() > 0) {
			String json = stringBuilder.toString();
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> map = objectMapper.readValue(json, Map.class);
			map.forEach((k, v) -> {
				if (v instanceof String) {
					v = cleanXSS((String) v);
					map.put(k, v);
				}
			});
			json = objectMapper.writeValueAsString(map);
			requestBody = json.getBytes();
		}
	}

	/**
	 * Handles arguments annotated by @RequestBody
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);
		return new ServletInputStream() {
			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setReadListener(ReadListener readListener) {

			}

			@Override
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}
		};
	}

	/**
	 * Handles arguments annotated by @RequestParam
	 *
	 * @param name
	 * @return
	 */
	@Override
	public String[] getParameterValues(String name) {
		String[] values = super.getParameterValues(name);
		if (values != null && values.length > 0) {
			String[] safeValues = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				safeValues[i] = cleanXSS(values[i]);
			}
			return safeValues;
		}
		return values;
	}

	/**
	 * Handles arguments annotated by @PathVariable
	 *
	 * @param name
	 * @return
	 */
	@Override
	public Object getAttribute(String name) {
		Object value = super.getAttribute(name);
		if (name.equalsIgnoreCase(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) && value != null && value instanceof Map) {
			((Map) value).forEach((k, v) -> {
				if (v instanceof String) {
					v = cleanXSS((String) v);
					((Map) value).put(k, v);
				}
			});
		}
		return value;
	}

	/**
	 * Handles arguments annotated by @RequestHeader
	 *
	 * @param name
	 * @return
	 */
	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> list = Collections.list(super.getHeaders(name));
		list = list.stream().map((e) -> {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				Map<String, String> map = objectMapper.readValue(e, Map.class);
				map.forEach((k, v) -> {
					v = cleanXSS(v);
					map.put(k, v);
				});
				e = objectMapper.writeValueAsString(map);
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			return e;
		}).collect(Collectors.toList());
		return Collections.enumeration(list);
	}

	@Override
	public String getParameter(String name) {
		String value = super.getParameter(name);
		if (value != null) {
			value = cleanXSS(value);
		}
		return value;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	/**
	 * Escape string to defend against XSS
	 *
	 * @param value
	 */
	private String cleanXSS(String value) {
		value = StringEscapeUtils.escapeHtml(value);
		return value;
	}
}
