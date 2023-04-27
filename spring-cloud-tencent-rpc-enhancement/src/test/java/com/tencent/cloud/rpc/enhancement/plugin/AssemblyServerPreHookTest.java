package com.tencent.cloud.rpc.enhancement.plugin;

import java.net.URI;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.cloud.rpc.enhancement.plugin.assembly.server.AssemblyServerPreHook;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class AssemblyServerPreHookTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	@InjectMocks
	private AssemblyServerPreHook assemblyServerPreHook;
	@Mock
	private AssemblyAPI assemblyAPI;

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

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testGetName() {
		assertThat(assemblyServerPreHook.getName()).isEqualTo(AssemblyServerPreHook.class.getName());
	}

	@Test
	public void testType() {
		assertThat(assemblyServerPreHook.getType()).isEqualTo(EnhancedPluginType.Server.PRE);
	}

	@Test
	public void testRun() {
		EnhancedPluginContext pluginContext = new EnhancedPluginContext();
		EnhancedRequestContext request = EnhancedRequestContext.builder()
				.httpMethod(HttpMethod.GET)
				.url(URI.create("http://0.0.0.0/"))
				.httpHeaders(new HttpHeaders())
				.build();
		request.toString();
		EnhancedResponseContext response = EnhancedResponseContext.builder()
				.httpStatus(200)
				.build();
		response.toString();

		DefaultServiceInstance targetServiceInstance = new DefaultServiceInstance();
		targetServiceInstance.setServiceId(SERVICE_PROVIDER);

		DefaultServiceInstance localServiceInstance = new DefaultServiceInstance();
		localServiceInstance.setServiceId(SERVICE_PROVIDER);

		pluginContext.setRequest(request);
		pluginContext.setResponse(response);
		pluginContext.setTargetServiceInstance(targetServiceInstance);
		pluginContext.setLocalServiceInstance(localServiceInstance);
		pluginContext.setThrowable(new RuntimeException());

		assemblyServerPreHook.run(pluginContext);
		assemblyServerPreHook.getOrder();
		assemblyServerPreHook.getName();
		assemblyServerPreHook.getType();
	}

	@Test
	public void testHandlerThrowable() {
		// mock request
		EnhancedRequestContext request = mock(EnhancedRequestContext.class);
		// mock response
		EnhancedResponseContext response = mock(EnhancedResponseContext.class);

		EnhancedPluginContext context = new EnhancedPluginContext();
		context.setRequest(request);
		context.setResponse(response);
		assemblyServerPreHook.handlerThrowable(context, new RuntimeException("Mock exception."));
	}

}
