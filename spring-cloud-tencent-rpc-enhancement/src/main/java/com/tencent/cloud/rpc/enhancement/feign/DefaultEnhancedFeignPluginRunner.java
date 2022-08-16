package com.tencent.cloud.rpc.enhancement.feign;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;

import org.springframework.util.CollectionUtils;

/**
 * Default plugin runner.
 *
 * @author Derek Yi 2022-08-16
 */
public class DefaultEnhancedFeignPluginRunner implements EnhancedFeignPluginRunner {

	private Multimap<String, EnhancedFeignPlugin> pluginMap = ArrayListMultimap.create();

	public DefaultEnhancedFeignPluginRunner(List<EnhancedFeignPlugin> enhancedFeignPlugins) {
		if (!CollectionUtils.isEmpty(enhancedFeignPlugins)) {
			enhancedFeignPlugins.stream()
					.sorted(Comparator.comparing(EnhancedFeignPlugin::getOrder))
					.forEach(plugin -> pluginMap.put(plugin.getType().name(), plugin));
		}
	}

	/**
	 * run the plugin.
	 *
	 * @param pluginType type of plugin
	 * @param context context in enhanced feign client.
	 */
	@Override
	public void run(EnhancedFeignPluginType pluginType, EnhancedFeignContext context) {
		for (EnhancedFeignPlugin plugin : pluginMap.get(pluginType.name())) {
			try {
				plugin.run(context);
			}
			catch (Throwable throwable) {
				plugin.handlerThrowable(context, throwable);
			}
		}
	}
}
