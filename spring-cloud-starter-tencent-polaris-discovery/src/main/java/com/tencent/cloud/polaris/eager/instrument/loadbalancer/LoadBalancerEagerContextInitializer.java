package com.tencent.cloud.polaris.eager.instrument.loadbalancer;


import java.util.List;

import com.tencent.polaris.api.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationListener;

/**
 * @author Yuwei Fu
 */
public class LoadBalancerEagerContextInitializer implements ApplicationListener<ApplicationReadyEvent> {


    private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerEagerContextInitializer.class);

    private final LoadBalancerClientFactory factory;

    private final List<String> serviceNames;

    public LoadBalancerEagerContextInitializer(LoadBalancerClientFactory factory, List<String> serviceNames) {
        this.factory = factory;
        this.serviceNames = serviceNames;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

        LOG.info("spring cloud eager-load start");
        try {
            if (!CollectionUtils.isEmpty(serviceNames)) {
                for (String serviceName : serviceNames) {
                    LoadBalancerWarmUpUtils.warmUp(factory, serviceName);
                }
            }
            LOG.info("spring cloud eager-load end");
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("spring cloud eager-load failed.", e);
            }
        }
    }
}
