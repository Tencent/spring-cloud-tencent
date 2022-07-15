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
				System.out.println("事件ApplicationStartingEvent");
				break;
			case "ApplicationEnvironmentPreparedEvent":
				System.out.println("事件ApplicationEnvironmentPreparedEvent");
				break;
			case "ApplicationContextInitializedEvent":
				System.out.println("事件ApplicationContextInitializedEvent");
				break;
			case "ApplicationPreparedEvent":
				System.out.println("事件ApplicationPreparedEvent");
				break;
			case "ContextRefreshedEvent":
				System.out.println("事件ContextRefreshedEvent");
				break;
			case "ApplicationStartedEvent":
				System.out.println("事件ApplicationStartedEvent");
				break;
			case "ServletWebServerInitializedEvent":
				if (event instanceof WebServerInitializedEvent) {
					System.out.println("事件WebServerInitializedEvent");
				}
				if (event instanceof ServletWebServerInitializedEvent) {
					System.out.println("事件ServletWebServerInitializedEvent");
				}
				if (event instanceof ReactiveWebServerInitializedEvent) {
					System.out.println("事件ReactiveWebServerInitializedEvent");
				}
				break;
			case "ApplicationReadyEvent":
				System.out.println("事件ApplicationReadyEvent");
				break;
			case "ApplicationFailedEvent":
				break;
		}
	}
}
