package com.tencent.cloud.polaris.config.config;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tencent.cloud.polaris.config.listener.ConfigChangeListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 *@author : wh
 *@date : 2022/7/10 23:09
 *@description:
 */
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware,
		ApplicationEventPublisherAware, PriorityOrdered {
	private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();
	private static final Set<BeanFactory> AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES = Sets.newConcurrentHashSet();

	private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector
			.getInstance(ConfigPropertySourceFactory.class);
	private ConfigUtil configUtil;
	private ConfigurableEnvironment environment;
	private ApplicationEventPublisher applicationEventPublisher;

	public static boolean addNamespaces(Collection<String> namespaces, int order) {
		return NAMESPACE_NAMES.putAll(order, namespaces);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
		initializePropertySources();
		initializeAutoUpdatePropertiesFeature(beanFactory);
	}

	private void initializePropertySources() {
		if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME)) {
			//already initialized
			return;
		}
		CompositePropertySource composite;
		if (configUtil.isPropertyNamesCacheEnabled()) {
			composite = new CachedCompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);
		} else {
			composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);
		}

		//sort by order asc
		ImmutableSortedSet<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
		Iterator<Integer> iterator = orders.iterator();

		while (iterator.hasNext()) {
			int order = iterator.next();
			for (String namespace : NAMESPACE_NAMES.get(order)) {
				Config config = ConfigService.getConfig(namespace);

				composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
			}
		}

		// clean up
		NAMESPACE_NAMES.clear();

		// add after the bootstrap property source or to the first
		if (environment.getPropertySources()
				.contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {

			if (configUtil.isOverrideSystemProperties()) {
				// ensure ApolloBootstrapPropertySources is still the first
				ensureBootstrapPropertyPrecedence(environment);
			}

			environment.getPropertySources()
					.addAfter(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME, composite);
		} else {
			if (!configUtil.isOverrideSystemProperties()) {
				if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
					environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, composite);
					return;
				}
			}
			environment.getPropertySources().addFirst(composite);
		}
	}

	private void ensureBootstrapPropertyPrecedence(ConfigurableEnvironment environment) {
		MutablePropertySources propertySources = environment.getPropertySources();

		PropertySource<?> bootstrapPropertySource = propertySources
				.get(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);

		// not exists or already in the first place
		if (bootstrapPropertySource == null || propertySources.precedenceOf(bootstrapPropertySource) == 0) {
			return;
		}

		propertySources.remove(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(bootstrapPropertySource);
	}

	private void initializeAutoUpdatePropertiesFeature(ConfigurableListableBeanFactory beanFactory) {
		if (!AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.add(beanFactory)) {
			return;
		}

		ConfigChangeListener configChangeEventPublisher = changeEvent ->
				applicationEventPublisher.publishEvent(new ApolloConfigChangeEvent(changeEvent));

		List<ConfigPropertySource> configPropertySources = configPropertySourceFactory.getAllConfigPropertySources();
		for (ConfigPropertySource configPropertySource : configPropertySources) {
			configPropertySource.addChangeListener(configChangeEventPublisher);
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		//it is safe enough to cast as all known environment is derived from ConfigurableEnvironment
		this.environment = (ConfigurableEnvironment) environment;
	}

	@Override
	public int getOrder() {
		//make it as early as possible
		return Ordered.HIGHEST_PRECEDENCE;
	}

	// for test only
	static void reset() {
		NAMESPACE_NAMES.clear();
		AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.clear();
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
