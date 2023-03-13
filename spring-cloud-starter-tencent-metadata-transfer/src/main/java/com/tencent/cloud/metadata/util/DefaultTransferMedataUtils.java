package com.tencent.cloud.metadata.util;

import java.util.HashMap;
import java.util.Map;


import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAME;
import static com.tencent.cloud.common.constant.MetadataConstant.DefaultMetadata.DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_NAMESPACE;
import static com.tencent.cloud.common.metadata.MetadataContext.LOCAL_SERVICE;

public final class DefaultTransferMedataUtils {

	private DefaultTransferMedataUtils() {
	}

	public static Map<String, String> getDefaultTransferMedata() {
		return new HashMap<String, String>() {{
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAMESPACE, LOCAL_NAMESPACE);
			put(DEFAULT_METADATA_SOURCE_SERVICE_NAME, LOCAL_SERVICE);
		}};
	}
}
