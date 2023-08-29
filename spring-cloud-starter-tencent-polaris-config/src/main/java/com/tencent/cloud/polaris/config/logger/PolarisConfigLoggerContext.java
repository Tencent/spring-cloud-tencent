/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.tencent.cloud.polaris.config.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;

import static org.springframework.boot.logging.LoggingSystem.ROOT_LOGGER_NAME;

/**
 * @author juanyinyang
 */
public final class PolarisConfigLoggerContext {

	private static LoggingSystem loggingSystem;

	private PolarisConfigLoggerContext() {

	}
	protected static void setLogSystem(LoggingSystem logSystem) {
		Assert.notNull(logSystem, "Logging System should not be null");
		PolarisConfigLoggerContext.loggingSystem = logSystem;
	}

	public static void setLevel(String loggerName, String level) {
		if (loggingSystem == null) {
			printLog("[SCT Config] PolarisConfigLoggerContext logger: [" + loggerName + "] change to target level fail. caused by internal exception:" + level, Level.WARN);
			return;
		}
		Level loggerLevel = Level.levelOf(level);
		if (loggerLevel == null) {
			printLog("[SCT Config] PolarisConfigLoggerContext logger: [" + loggerName + "] change to target level fail. caused by level is not support, level:" + level, Level.WARN);
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
				printLog("[SCT Config] PolarisConfigLoggerContext logger: [" + loggerName + "] setLevel fail. caused by level is not support, level: " + level, Level.WARN);
		}
		loggingSystem.setLogLevel(loggerName, logLevel);
		printLog("[SCT Config] PolarisConfigLoggerContext logger: [" + loggerName + "] changed to level:" + level, Level.INFO);
	}
	/**
	 * print log.
	 */
	private static void printLog(String message, Level level) {
		Logger logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME);
		if (level.ordinal() <= Level.INFO.ordinal()) {
			if (logger != null) {
				logger.info(message);
			}
			else {
				StdLog.info(message);
			}
		}
		else {
			if (logger != null) {
				logger.warn(message);
			}
			else {
				StdLog.warn(message);
			}
		}
	}
}
