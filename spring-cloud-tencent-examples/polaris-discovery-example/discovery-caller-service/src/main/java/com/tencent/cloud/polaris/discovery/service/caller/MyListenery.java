package com.tencent.cloud.polaris.discovery.service.caller;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class MyListenery implements ApplicationListener {
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
//        ApplicationStartingEvent//启动开始的时候执行的事件
//        ApplicationEnvironmentPreparedEvent//上下文创建之前运行的事件
//        ApplicationContextInitializedEvent//
//        ApplicationPreparedEvent//上下文创建完成，注入的bean还没加载完成
//        ContextRefreshedEvent//上下文刷新
//        ServletWebServerInitializedEvent//web服务器初始化
//        ApplicationStartedEvent//
//        ApplicationReadyEvent//启动成功
//        ApplicationFailedEvent//在启动Spring发生异常时触发
        switch (event.getClass().getSimpleName()){
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
                if( event instanceof WebServerInitializedEvent){
                    System.out.println("事件WebServerInitializedEvent");
                }
                if( event instanceof ServletWebServerInitializedEvent){
                    System.out.println("事件ServletWebServerInitializedEvent");
                }
                if( event instanceof ReactiveWebServerInitializedEvent){
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