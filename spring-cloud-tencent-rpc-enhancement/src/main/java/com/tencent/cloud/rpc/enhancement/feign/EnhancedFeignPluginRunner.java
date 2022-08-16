package com.tencent.cloud.rpc.enhancement.feign;

import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;

/**
 * Plugin runner.
 *
 * @author Derek Yi 2022-08-16
 */
public interface EnhancedFeignPluginRunner {

	/**
	 * run the plugin.
	 *
	 * @param pluginType type of plugin
	 * @param context context in enhanced feign client.
	 */
	void run(EnhancedFeignPluginType pluginType, EnhancedFeignContext context);
}
