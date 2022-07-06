package com.tencent.cloud.polaris.context;

import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.route.LocationLevel;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;


/**
 *@author : wh
 *@date : 2022/7/6 11:41
 *@description:
 */
@RunWith(MockitoJUnitRunner.class)
public class PostInitPolarisSDKContextTest {

	@Mock
	private SDKContext sdkContext;
	@Mock
	private StaticMetadataManager staticMetadataManager;

	private static final String region = "region";

	private static final String zone = "zone";

	private static final String campus = "campus";

	@Test
	public void PostInitPolarisSDKContextTest() {
		ValueContext valueContext = new ValueContext();

		when(sdkContext.getValueContext()).thenReturn(valueContext);
		when(staticMetadataManager.getRegion()).thenReturn(region);
		when(staticMetadataManager.getZone()).thenReturn(zone);
		when(staticMetadataManager.getCampus()).thenReturn(campus);

		new PostInitPolarisSDKContext(sdkContext, staticMetadataManager);
		assertThat(valueContext.getValue(LocationLevel.region.name()), is(region));
		assertThat(valueContext.getValue(LocationLevel.zone.name()), is(zone));
		assertThat(valueContext.getValue(LocationLevel.campus.name()), is(campus));
	}
}
