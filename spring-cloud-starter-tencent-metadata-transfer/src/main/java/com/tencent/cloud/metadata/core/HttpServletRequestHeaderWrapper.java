package com.tencent.cloud.metadata.core;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;


public class HttpServletRequestHeaderWrapper extends HttpServletRequestWrapper {

	private  Map<String, String> addHeaders;

	public HttpServletRequestHeaderWrapper(HttpServletRequest request,  Map<String, String> addHeaders) {
		super(request);
		this.addHeaders = addHeaders;
	}


	@Override
	public String getHeader(String name) {
		if (addHeaders.containsKey(name)) {
			return addHeaders.get(name);
		}
		else  {
			return super.getHeader(name);
		}
	}
}
