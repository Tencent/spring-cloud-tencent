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
 */

package com.tencent.cloud.polaris.context.logging;

import com.tencent.cloud.polaris.context.PolarisContextApplication;
import com.tencent.polaris.logging.LoggingConsts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link PolarisLoggingApplicationListener}
 *
 * @author wenxuan70
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PolarisContextApplication.class,
		properties = {"spring.config.location=classpath:bootstrap.yml"})
public class PolarisLoggingPathSystemPropertyTest {

	@Test
	public void testSystemProperty() {
		assertThat(System.getProperty(LoggingConsts.LOGGING_PATH_PROPERTY)).isEqualTo("/tmp/polaris/logs");
	}
}
