package com.tencent.cloud.rpc.enhancement.plugin;

import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.AssemblyResponseContext;
import com.tencent.polaris.api.pojo.RetStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class AssemblyResponseContextTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		RpcEnhancementReporterProperties reporterProperties = mock(RpcEnhancementReporterProperties.class);
		doReturn(reporterProperties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@Test
	public void testAssemblyResponseContext() {

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("a", "a");

		EnhancedResponseContext enhancedResponseContext = EnhancedResponseContext.builder()
				.httpHeaders(httpHeaders)
				.httpStatus(HttpStatus.OK.value())
				.build();

		AssemblyResponseContext assemblyResponseContext = new AssemblyResponseContext(enhancedResponseContext, null);
		assertThat(assemblyResponseContext.getHeader("a")).isEqualTo("a");
		assertThat(assemblyResponseContext.getRetCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(assemblyResponseContext.getThrowable()).isEqualTo(null);
		assertThat(assemblyResponseContext.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
		assertThat(assemblyResponseContext.listHeaders()).isEqualTo(new HashSet<>(List.of("a")));

		Throwable e = new SocketTimeoutException();
		assemblyResponseContext = new AssemblyResponseContext(null, e);
		assertThat(assemblyResponseContext.getHeader("a")).isEqualTo(null);
		assertThat(assemblyResponseContext.getRetCode()).isEqualTo(null);
		assertThat(assemblyResponseContext.getThrowable()).isEqualTo(e);
		assertThat(assemblyResponseContext.getRetStatus()).isEqualTo(RetStatus.RetTimeout);
		assertThat(assemblyResponseContext.listHeaders()).isEqualTo(null);

	}

}
