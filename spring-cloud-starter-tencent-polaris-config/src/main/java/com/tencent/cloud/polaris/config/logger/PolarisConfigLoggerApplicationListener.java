package com.tencent.cloud.polaris.config.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author juanyinyang
 */
public class PolarisConfigLoggerApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisConfigLoggerApplicationListener.class);
    
    /** 
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {
            // Initialize application loggingSystem.
            if (event instanceof ApplicationStartedEvent) {
                ApplicationStartedEvent startedEvent = (ApplicationStartedEvent) event;
                ClassLoader classLoader = startedEvent.getSpringApplication().getClassLoader();
                LoggingSystem loggingSystem = LoggingSystem.get(classLoader);
                LOGGER.info("PolarisConfigLoggerApplicationListener onApplicationEvent init loggingSystem:{}", loggingSystem);
                PolarisConfigLoggerContext.setLogSystem(loggingSystem);
            } 
        } catch (Exception e) {
            LOGGER.error("PolarisConfigLoggerApplicationListener onApplicationEvent exception:", e);
        }
    }
}
