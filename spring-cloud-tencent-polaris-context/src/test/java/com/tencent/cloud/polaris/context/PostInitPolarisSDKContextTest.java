package com.tencent.cloud.polaris.context;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.route.LocationLevel;
import com.tencent.polaris.client.api.SDKContext;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;


/**
 * Test for {@link PostInitPolarisSDKContext}.
 *
 * @author wh
 */
@RunWith(MockitoJUnitRunner.class)
public final class PostInitPolarisSDKContextTest {

	@Mock
	private SDKContext sdkContext;
	@Mock
	private StaticMetadataManager staticMetadataManager;

	private static final String REGION = "region";

	private static final String ZONE = "zone";

	private static final String CAMPUS = "campus";

	@Test
	public void PostInitPolarisSDKContextTest() {
		ValueContext valueContext = new ValueContext();

		when(sdkContext.getValueContext()).thenReturn(valueContext);
		when(staticMetadataManager.getRegion()).thenReturn(REGION);
		when(staticMetadataManager.getZone()).thenReturn(ZONE);
		when(staticMetadataManager.getCampus()).thenReturn(CAMPUS);

		new PostInitPolarisSDKContext(sdkContext, staticMetadataManager);
		String regionName = valueContext.getValue(LocationLevel.region.name());
		String zoneName = valueContext.getValue(LocationLevel.zone.name());
		String campusName = valueContext.getValue(LocationLevel.campus.name());

		Assertions.assertThat(regionName).isEqualTo(REGION);
		Assertions.assertThat(zoneName).isEqualTo(ZONE);
		Assertions.assertThat(campusName).isEqualTo(CAMPUS);
	}
}
