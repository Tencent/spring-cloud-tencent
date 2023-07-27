/*
 * Copyright (c) 2018 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.cloud.polaris.config.logger;

import static org.springframework.boot.logging.LoggingSystem.ROOT_LOGGER_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;


/**
 * @author juanyinyang
 */
public final class PolarisConfigLoggerContext {

    private static LoggingSystem loggingSystem;

    private PolarisConfigLoggerContext() {

    }

    protected static void setLogSystem(LoggingSystem logSystem) {
        Assert.notNull(logSystem,"Logging System should not be null");
        PolarisConfigLoggerContext.loggingSystem = logSystem;
    }

    public static void setLevel(String loggerName, String level) {
       if (loggingSystem == null) {
           printLog("[SCT Config] PolarisConfigLoggerContext logger: ["+loggerName+"] change to target level fail. caused by internal exception:" + level,Level.WARN);
           return;
       }
       Level loggerLevel = Level.levelOf(level);
       if (loggerLevel == null) {
           printLog("[SCT Config] PolarisConfigLoggerContext logger: ["+loggerName+"] change to target level fail. caused by level is not support, level:" + level,Level.WARN);
           return;
       }
       LogLevel logLevel = null;
       switch (loggerLevel) {
           case TRACE:
               logLevel = LogLevel.TRACE;
               break;
           case DEBUG:
               logLevel = LogLevel.DEBUG;
               break;
           case OFF:
               logLevel = LogLevel.OFF;
               break;
           case INFO:
               logLevel = LogLevel.INFO;
               break;
           case WARN:
               logLevel = LogLevel.WARN;
               break;
           case ERROR:
               logLevel = LogLevel.ERROR;
               break;
           case FATAL:
               logLevel = LogLevel.FATAL;
               break;
           default:
               printLog("[SCT Config] PolarisConfigLoggerContext logger: ["+loggerName+"] setLevel fail. caused by level is not support, level: " + level,Level.WARN);
       }
       loggingSystem.setLogLevel(loggerName,logLevel);
       printLog("[SCT Config] PolarisConfigLoggerContext logger: ["+loggerName+"] changed to level:" + level,Level.INFO);
    }

    /**
     * 打印日志
     * @param message
     * @param level
     */
    private static void printLog(String message, Level level){
        Logger logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME);
        if (level.ordinal() <= Level.INFO.ordinal() ) {
            if (logger != null) {
                logger.info(message);
            } else {
                StdLog.info(message);
            }
        } else {
            if (logger != null) {
                logger.warn(message);
            } else {
                StdLog.warn(message);
            }
        }
    }
}
