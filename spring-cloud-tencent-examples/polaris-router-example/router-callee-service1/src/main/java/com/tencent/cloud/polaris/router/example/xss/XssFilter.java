package com.tencent.cloud.polaris.router.example.xss;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * filter request aim at defending against XSS
 *
 * @author Daifu Wu
 */
@WebFilter(urlPatterns = "/*", filterName = "xssFilter")
@Component
public class XssFilter implements Filter {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		filterChain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) servletRequest), servletResponse);
	}
}
