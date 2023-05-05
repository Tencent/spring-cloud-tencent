package com.tencent.cloud.rpc.enhancement.plugin;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.rpc.enhancement.plugin.assembly.AssemblyMetadataProvider;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.ServiceInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class AssemblyMetadataProviderTest {

	@Test
	public void testAssemblyMetadataProvider() {
		ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);
		Map<String, String> metadata = new HashMap<>() {{
			put("k", "v");
		}};
		doReturn(metadata).when(serviceInstance).getMetadata();
		doReturn("0.0.0.0").when(serviceInstance).getHost();
		doReturn("test").when(serviceInstance).getServiceId();
		AssemblyMetadataProvider assemblyMetadataProvider = new AssemblyMetadataProvider(serviceInstance, "test");
		assertThat(assemblyMetadataProvider.getMetadata("k")).isEqualTo("v");
		assertThat(assemblyMetadataProvider.getLocalIp()).isEqualTo("0.0.0.0");
		assertThat(assemblyMetadataProvider.getLocalService()).isEqualTo(new ServiceKey("test", "test"));
	}
}
