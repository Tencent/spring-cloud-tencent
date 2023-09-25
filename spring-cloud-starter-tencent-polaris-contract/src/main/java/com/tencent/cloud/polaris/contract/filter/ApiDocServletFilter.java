package com.tencent.cloud.polaris.contract.filter;

import java.io.IOException;

import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_RESOURCE_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V2_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V3_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V2_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V3_PREFIX;
import static org.springdoc.core.utils.Constants.SWAGGER_UI_URL;

/**
 * Filter to disable api doc controller.
 *
 * @author Haotian Zhang
 */
@WebFilter
public class ApiDocServletFilter extends OncePerRequestFilter {

	private final PolarisContractProperties polarisContractProperties;

	public ApiDocServletFilter(PolarisContractProperties polarisContractProperties) {
		this.polarisContractProperties = polarisContractProperties;
	}

	@Override
	public void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
			@NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		if (!polarisContractProperties.isExposure()) {
			String path = httpServletRequest.getServletPath();
			if (path.equals(SWAGGER_V2_API_DOC_URL) ||
					path.startsWith(SWAGGER_V3_API_DOC_URL) ||
					path.equals(SWAGGER_UI_URL) ||
					path.startsWith(SWAGGER_RESOURCE_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V2_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V3_PREFIX)) {
				httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}
}

