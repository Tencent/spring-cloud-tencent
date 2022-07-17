package com.tencent.cloud.polaris.discovery.service.caller;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class SpringEventOrderListener implements ApplicationListener {

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		switch (event.getClass().getSimpleName()) {
			case "ApplicationStartingEvent":
				System.out.println("Event ApplicationStartingEvent occurred");
				break;
			case "ApplicationEnvironmentPreparedEvent":
				System.out.println("Event ApplicationEnvironmentPreparedEvent occurred");
				break;
			case "ApplicationContextInitializedEvent":
				System.out.println("Event ApplicationContextInitializedEvent occurred");
				break;
			case "ApplicationPreparedEvent":
				System.out.println("Event ApplicationPreparedEvent occurred");
				break;
			case "ContextRefreshedEvent":
				System.out.println("Event ContextRefreshedEvent occurred");
				break;
			case "ApplicationStartedEvent":
				System.out.println("Event ApplicationStartedEvent occurred");
				break;
			case "AvailabilityChangeEvent":
				System.out.println("Event AvailabilityChangeEvent occurred");
				break;
			case "ServletWebServerInitializedEvent":
				if (event instanceof WebServerInitializedEvent) {
					System.out.println("Event WebServerInitializedEvent occurred");
				}
				if (event instanceof ServletWebServerInitializedEvent) {
					System.out.println("Event ServletWebServerInitializedEvent occurred");
				}
				if (event instanceof ReactiveWebServerInitializedEvent) {
					System.out.println("Event ReactiveWebServerInitializedEvent occurred");
				}
				break;
			case "ApplicationReadyEvent":
				System.out.println("Event ApplicationReadyEvent occurred");
				break;
			case "ApplicationFailedEvent":
				System.out.println("Event ApplicationFailedEvent occurred");
				break;
		}
	}
}
