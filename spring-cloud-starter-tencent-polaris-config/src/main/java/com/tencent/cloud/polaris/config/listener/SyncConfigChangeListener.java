package com.tencent.cloud.polaris.config.listener;

public interface SyncConfigChangeListener extends ConfigChangeListener {
	default boolean isAsync() {
		return true;
	}
}
