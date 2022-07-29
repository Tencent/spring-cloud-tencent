package com.tencent.cloud.polaris.circuitbreaker.example.xss;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Escape String in ResponseBody before write it into HttpResponse
 *
 * @author Daifu Wu
 */
@ControllerAdvice
public class XssResponseBodyAdvice implements ResponseBodyAdvice {

	@Override
	public boolean supports(MethodParameter methodParameter, Class aClass) {
		return methodParameter.hasMethodAnnotation(ResponseBody.class) || methodParameter.getDeclaringClass().getAnnotation(ResponseBody.class) != null || methodParameter.getDeclaringClass().getAnnotation(RestController.class) != null;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter methodParameter, MediaType mediaType, Class aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
		if (body instanceof String) {
			body = StringEscapeUtils.escapeHtml((String)body);
			return body;
		}
		try {
			if (!((Class)body.getClass().getField("TYPE").get(null)).isPrimitive()) {
				Map<String, Object> map = new HashMap<>();
				Field[] fields = body.getClass().getDeclaredFields();
				for (Field field: fields) {
					field.setAccessible(true);
					Object value = field.get(body);
					if (value instanceof String) {
						value = StringEscapeUtils.escapeHtml((String) value);
					}
					map.put(field.getName(), value);
				}
				return map;
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return body;
	}
}
